from __future__ import annotations

from datetime import date, datetime, timedelta, timezone
from typing import Any

from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncSession

MIN_CONNECTION_MINUTES = 40
MAX_CONNECTION_HOURS = 24


def _day_bounds_utc(d: date) -> tuple[datetime, datetime]:
    start = datetime(d.year, d.month, d.day, tzinfo=timezone.utc)
    end = start + timedelta(days=1)
    return start, end


def _normalize_booking_class(booking_class: str | None) -> str | None:
    if booking_class is None:
        return None
    return booking_class.strip().capitalize()


def _build_start_class_filter() -> str:
    return """
    EXISTS (
        SELECT 1
        FROM bookings.seats s
        WHERE s.airplane_code = r.airplane_code
          AND s.fare_conditions = :booking_class
    )
    """


def _build_connection_class_filter() -> str:
    return """
    EXISTS (
        SELECT 1
        FROM bookings.seats s
        WHERE s.airplane_code = r2.airplane_code
          AND s.fare_conditions = :booking_class
    )
    """


async def search_routes(
    session: AsyncSession,
    from_airport: str | None,
    from_city: str | None,
    to_airport: str | None,
    to_city: str | None,
    departure_date: date,
    max_connections: int,
    booking_class: str | None = None,
    from_country: str | None = None,
    to_country: str | None = None,
    min_connection_minutes: int = MIN_CONNECTION_MINUTES,
    max_connection_hours: int = MAX_CONNECTION_HOURS,
) -> list[dict[str, Any]]:
    if from_airport is None and from_city is None:
        return []

    if to_airport is None and to_city is None:
        return []

    unbound = max_connections < 0

    day_start, day_end = _day_bounds_utc(departure_date)
    normalized_class = _normalize_booking_class(booking_class)

    params: dict[str, Any] = {
        "day_start": day_start,
        "day_end": day_end,
        "max_connections": int(max_connections),
    }

    start_conditions: list[str] = [
        "f.scheduled_departure >= :day_start",
        "f.scheduled_departure < :day_end",
        "f.scheduled_departure <@ r.validity",
        "f.status = 'Scheduled'",
    ]

    if from_airport is not None:
        start_conditions.append("r.departure_airport = :departure_airport")
        params["departure_airport"] = from_airport

    if from_city is not None:
        start_conditions.append("dep_airport.city->>'en' ILIKE :departure_city")
        params["departure_city"] = f"%{from_city}%"

    if from_country is not None:
        start_conditions.append("dep_airport.country->>'en' = :departure_country")
        params["departure_country"] = from_country

    if normalized_class is not None:
        start_conditions.append(_build_start_class_filter())
        params["booking_class"] = normalized_class

    finish_conditions: list[str] = []

    if to_airport is not None:
        finish_conditions.append("arrival_airport = :arrival_airport")
        params["arrival_airport"] = to_airport

    if to_city is not None:
        finish_conditions.append("arrival_city ILIKE :arrival_city")
        params["arrival_city"] = f"%{to_city}%"

    if to_country is not None:
        finish_conditions.append("arrival_country = :arrival_country")
        params["arrival_country"] = to_country

    connection_conditions: list[str] = [
        f"f2.scheduled_departure >= i.scheduled_arrival + interval '{min_connection_minutes} minutes'",
        f"f2.scheduled_departure <= i.scheduled_arrival + interval '{max_connection_hours} hours'",
        "NOT (arr_airport2.city->>'en' = ANY(i.path_cities))",
        "f2.scheduled_departure <@ r2.validity",
        "f2.status = 'Scheduled'",
    ]

    if not unbound:
        connection_conditions.append("(i.connections + 1 <= :max_connections)")

    if normalized_class is not None:
        connection_conditions.append(_build_connection_class_filter())

    sql = f"""
    WITH RECURSIVE itins AS (
        SELECT
            f.flight_id,
            r.departure_airport,
            dep_airport.city->>'en' AS departure_city,
            f.scheduled_departure,
            r.arrival_airport,
            arr_airport.city->>'en' AS arrival_city,
            arr_airport.country->>'en' AS arrival_country,
            f.scheduled_arrival,
            0 AS connections,
            ARRAY[dep_airport.city->>'en', arr_airport.city->>'en']::text[] AS path_cities,
            ARRAY[r.departure_airport::text, r.arrival_airport::text]::text[] AS path_airports,
            ARRAY[f.flight_id::bigint]::bigint[] AS path_flights
        FROM bookings.flights f
        JOIN bookings.routes r
            ON r.route_no = f.route_no
        JOIN bookings.airports_data dep_airport
            ON dep_airport.airport_code = r.departure_airport
        JOIN bookings.airports_data arr_airport
            ON arr_airport.airport_code = r.arrival_airport
        WHERE {" AND ".join(start_conditions)}

        UNION ALL

        SELECT
            f2.flight_id,
            i.departure_airport,
            i.departure_city,
            i.scheduled_departure,
            r2.arrival_airport,
            arr_airport2.city->>'en' AS arrival_city,
            arr_airport2.country->>'en' AS arrival_country,
            f2.scheduled_arrival,
            i.connections + 1 AS connections,
            (i.path_cities || (arr_airport2.city->>'en'))::text[] AS path_cities,
            (i.path_airports || r2.arrival_airport::text)::text[] AS path_airports,
            (i.path_flights || f2.flight_id::bigint)::bigint[] AS path_flights
        FROM itins i
        JOIN bookings.routes r2
            ON r2.departure_airport = i.arrival_airport
        JOIN bookings.flights f2
            ON f2.route_no = r2.route_no
        JOIN bookings.airports_data arr_airport2
            ON arr_airport2.airport_code = r2.arrival_airport
        WHERE {" AND ".join(connection_conditions)}
    )

    SELECT
        connections,
        path_flights
    FROM itins
    WHERE {" AND ".join(finish_conditions)}
    ORDER BY connections, path_flights;
    """

    rows = (await session.execute(text(sql), params)).mappings().all()

    result: list[dict[str, Any]] = []

    for row in rows:
        flight_ids = list(row["path_flights"])
        if not flight_ids:
            continue

        details_sql = text("""
            SELECT DISTINCT ON (f.flight_id)
                f.flight_id,
                f.route_no,
                r.departure_airport,
                r.arrival_airport,
                f.scheduled_departure,
                f.scheduled_arrival
            FROM bookings.flights f
            JOIN bookings.routes r
              ON r.route_no = f.route_no
             AND f.scheduled_departure <@ r.validity
            WHERE f.flight_id = ANY(:ids)
            ORDER BY f.flight_id, array_position(:ids, f.flight_id)
        """)

        detail_rows = (
            await session.execute(details_sql, {"ids": flight_ids})
        ).mappings().all()

        segments = []
        total_price = 0.0

        for d in detail_rows:
            price = await _get_flight_price(
                session,
                d["route_no"],
                d["scheduled_departure"],
                booking_class
            )
            total_price += price

            segments.append({
                "flightId": d["flight_id"],
                "flightNo": d["route_no"],
                "departureAirport": d["departure_airport"],
                "arrivalAirport": d["arrival_airport"],
                "departureTime": d["scheduled_departure"],
                "arrivalTime": d["scheduled_arrival"],
                "price": price,
            })

        result.append({
            "connectionsCount": int(row["connections"]),
            "segments": segments,
            "totalPrice": total_price,
        })

    return result


async def _get_flight_price(
    session: AsyncSession,
    route_no: str,
    departure_time: datetime,
    booking_class: str | None
) -> float:
    if booking_class is None:
        booking_class = "Economy"

    month = departure_time.month
    dow = departure_time.isoweekday()

    sql = text("""
        SELECT base_price
        FROM bookings.pricing_rules
        WHERE route_no = :route_no
          AND fare_conditions = :booking_class
          AND departure_month = :month
          AND day_of_week = :dow
        LIMIT 1
    """)

    result = await session.execute(sql, {
        "route_no": route_no,
        "booking_class": booking_class,
        "month": month,
        "dow": dow,
    })

    row = result.scalar_one_or_none()
    return float(row) if row else 10000.0