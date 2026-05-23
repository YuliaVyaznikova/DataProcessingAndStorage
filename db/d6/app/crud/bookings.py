from datetime import datetime, timedelta, timezone
import random
import string

from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncSession


def _generate_book_ref() -> str:
    chars = string.ascii_uppercase + string.digits
    return ''.join(random.choices(chars, k=6))


def _generate_ticket_no() -> str:
    return ''.join(random.choices(string.digits, k=13))


async def _unique_book_ref(session: AsyncSession) -> str:
    for _ in range(50):
        book_ref = _generate_book_ref()
        exists = (await session.execute(
            text("SELECT 1 FROM bookings.bookings WHERE book_ref = :br LIMIT 1"),
            {"br": book_ref},
        )).first()
        if not exists:
            return book_ref
    raise RuntimeError("Failed to generate unique book_ref")


async def _unique_ticket_no(session: AsyncSession) -> str:
    for _ in range(50):
        ticket_no = _generate_ticket_no()
        exists = (await session.execute(
            text("SELECT 1 FROM bookings.tickets WHERE ticket_no = :t LIMIT 1"),
            {"t": ticket_no},
        )).first()
        if not exists:
            return ticket_no
    raise RuntimeError("Failed to generate unique ticket_no")


async def _check_seat_availability(session: AsyncSession, flight_id: int, fare_conditions: str) -> None:
    sql = text("""
        SELECT
            (SELECT COUNT(*)
             FROM bookings.seats s
             JOIN bookings.routes r ON r.airplane_code = s.airplane_code
             JOIN bookings.flights f ON f.route_no = r.route_no
             WHERE f.flight_id = :flight_id
               AND s.fare_conditions = :fare_conditions
            ) AS total_seats,
            (SELECT COUNT(*)
             FROM bookings.segments sg
             WHERE sg.flight_id = :flight_id
               AND sg.fare_conditions = :fare_conditions
            ) AS sold_segments
    """)
    row = (await session.execute(sql, {
        "flight_id": flight_id,
        "fare_conditions": fare_conditions,
    })).mappings().one()

    total = int(row["total_seats"])
    sold = int(row["sold_segments"])

    if sold >= total:
        raise ValueError(f"No available seats for flight {flight_id} in class {fare_conditions}")


async def create_booking(
    session: AsyncSession,
    passenger_id: str,
    passenger_name: str,
    flight_ids: list[int],
    booking_class: str,
) -> dict:
    if len(flight_ids) != len(set(flight_ids)):
        raise ValueError("Duplicate flight_ids in booking request")

    await session.execute(text("SET TRANSACTION ISOLATION LEVEL REPEATABLE READ"))

    for flight_id in flight_ids:
        await _check_seat_availability(session, flight_id, booking_class)

    book_ref = await _unique_book_ref(session)
    ticket_no = await _unique_ticket_no(session)

    now = datetime.now(timezone.utc)

    total_amount = 0.0
    segments_data = []

    prev_airport = None
    for flight_id in flight_ids:
        flight_sql = text("""
            SELECT f.route_no, f.scheduled_departure, f.scheduled_arrival,
                   r.departure_airport, r.arrival_airport
            FROM bookings.flights f
            JOIN bookings.routes r ON f.route_no = r.route_no
                                  AND f.scheduled_departure <@ r.validity
            WHERE f.flight_id = :flight_id
        """)
        flight_row = (await session.execute(flight_sql, {"flight_id": flight_id})).mappings().one_or_none()

        if not flight_row:
            raise ValueError(f"Flight {flight_id} not found or not valid")

        if prev_airport is not None and flight_row["departure_airport"] != prev_airport:
            raise ValueError(
                f"Flight {flight_id} departure airport "
                f"({flight_row['departure_airport']}) does not match "
                f"previous flight's arrival airport ({prev_airport})"
            )
        prev_airport = flight_row["arrival_airport"]

        price_sql = text("""
            SELECT base_price
            FROM bookings.pricing_rules
            WHERE route_no = :route_no
              AND fare_conditions = :booking_class
              AND departure_month = :month
              AND day_of_week = :dow
            LIMIT 1
        """)

        month = flight_row["scheduled_departure"].month
        dow = flight_row["scheduled_departure"].isoweekday()

        price_row = await session.execute(price_sql, {
            "route_no": flight_row["route_no"],
            "booking_class": booking_class,
            "month": month,
            "dow": dow,
        })
        price = price_row.scalar_one_or_none() or 10000.0
        total_amount += float(price)

        segments_data.append({
            "flight_id": flight_id,
            "price": price,
        })

    insert_booking_sql = text("""
        INSERT INTO bookings.bookings (book_ref, book_date, total_amount)
        VALUES (:book_ref, :book_date, :total_amount)
    """)
    await session.execute(insert_booking_sql, {
        "book_ref": book_ref,
        "book_date": now,
        "total_amount": total_amount,
    })

    insert_ticket_sql = text("""
        INSERT INTO bookings.tickets (ticket_no, book_ref, passenger_id, passenger_name, outbound)
        VALUES (:ticket_no, :book_ref, :passenger_id, :passenger_name, :outbound)
    """)
    await session.execute(insert_ticket_sql, {
        "ticket_no": ticket_no,
        "book_ref": book_ref,
        "passenger_id": passenger_id,
        "passenger_name": passenger_name,
        "outbound": True,
    })

    for seg in segments_data:
        insert_segment_sql = text("""
            INSERT INTO bookings.segments (ticket_no, flight_id, fare_conditions, price)
            VALUES (:ticket_no, :flight_id, :fare_conditions, :price)
        """)
        await session.execute(insert_segment_sql, {
            "ticket_no": ticket_no,
            "flight_id": seg["flight_id"],
            "fare_conditions": booking_class,
            "price": seg["price"],
        })

    await session.commit()

    return {
        "bookRef": book_ref,
        "ticketNo": ticket_no,
        "totalAmount": total_amount,
    }
