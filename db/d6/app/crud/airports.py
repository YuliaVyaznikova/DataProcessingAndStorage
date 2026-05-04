from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncSession

SQL_CITIES = text("""
WITH params AS (
    SELECT (SELECT MAX(actual_departure) FROM bookings.flights) AS now_ts
)
SELECT DISTINCT
    city->>'en' AS city,
    country->>'en' AS country
FROM bookings.airports_data
WHERE airport_code IN (
    SELECT departure_airport
    FROM bookings.routes
    WHERE (SELECT now_ts FROM params) <@ validity
)
OR airport_code IN (
    SELECT arrival_airport
    FROM bookings.routes
    WHERE (SELECT now_ts FROM params) <@ validity
)
ORDER BY country, city;
""")

SQL_AIRPORTS = text("""
WITH params AS (
    SELECT (SELECT MAX(actual_departure) FROM bookings.flights) AS now_ts
)
SELECT 
    airport_code,
    airport_name->>'en' AS airport,
    city->>'en' AS city,
    country->>'en' AS country
FROM bookings.airports_data
WHERE airport_code IN (
    SELECT departure_airport FROM bookings.routes WHERE (SELECT now_ts FROM params) <@ validity
)
OR airport_code IN (
    SELECT arrival_airport FROM bookings.routes WHERE (SELECT now_ts FROM params) <@ validity
)
ORDER BY country, city, airport;
""")

SQL_CITY_AIRPORTS_BASE = """
SELECT 
    airport_code,
    airport_name->>'en' AS airport,
    city->>'en' AS city,
    country->>'en' AS country
FROM bookings.airports_data
WHERE city->>'en' = :city
"""

SQL_CITY_AIRPORTS_ORDER = " ORDER BY country, city, airport;"

SQL_INBOUND = text("""
WITH params AS (
    SELECT (SELECT MAX(actual_departure) FROM bookings.flights) AS now_ts
)
SELECT 
    r.route_no,
    r.days_of_week,
    (((DATE '2000-01-15'::timestamp + r.scheduled_time)
        AT TIME ZONE origin.timezone
        + r.duration
    ) AT TIME ZONE destination.timezone)::time AS arrival_time,
    origin.airport_code AS origin_airport,
    origin.airport_name->>'en' AS origin_name,
    origin.city->>'en' AS origin_city,
    origin.country->>'en' AS origin_country
FROM bookings.routes r
INNER JOIN bookings.airports_data origin ON r.departure_airport = origin.airport_code
INNER JOIN bookings.airports_data destination ON r.arrival_airport = destination.airport_code
WHERE (SELECT now_ts FROM params) <@ r.validity
  AND r.arrival_airport = :arrival_airport
ORDER BY r.route_no;
""")

SQL_OUTBOUND = text("""
WITH params AS (
    SELECT (SELECT MAX(actual_departure) FROM bookings.flights) AS now_ts
)
SELECT 
    r.route_no,
    r.days_of_week,
    ((DATE '2000-01-15'::timestamp + r.scheduled_time)
        AT TIME ZONE origin.timezone
    ) AT TIME ZONE origin.timezone AS departure_time,
    destination.airport_code AS destination_airport,
    destination.airport_name->>'en' AS destination_name,
    destination.city->>'en' AS destination_city,
    destination.country->>'en' AS destination_country
FROM bookings.routes r
INNER JOIN bookings.airports_data origin ON r.departure_airport = origin.airport_code
INNER JOIN bookings.airports_data destination ON r.arrival_airport = destination.airport_code
WHERE (SELECT now_ts FROM params) <@ r.validity
  AND r.departure_airport = :departure_airport
ORDER BY r.route_no;
""")


async def list_cities(session: AsyncSession):
    rows = (await session.execute(SQL_CITIES)).mappings().all()
    return [{"city": r["city"], "country": r["country"]} for r in rows]


async def list_airports(session: AsyncSession):
    rows = (await session.execute(SQL_AIRPORTS)).mappings().all()
    return [
        {
            "airportCode": r["airport_code"],
            "airportName": r["airport"],
            "city": r["city"],
            "country": r["country"],
        }
        for r in rows
    ]


async def airports_in_city(session: AsyncSession, city: str, country: str | None):
    sql = SQL_CITY_AIRPORTS_BASE
    params = {"city": city}

    if country is not None:
        sql += " AND country->>'en' = :country"
        params["country"] = country

    sql += SQL_CITY_AIRPORTS_ORDER

    rows = (await session.execute(text(sql), params)).mappings().all()
    return [
        {
            "airportCode": r["airport_code"],
            "airportName": r["airport"],
            "city": r["city"],
            "country": r["country"],
        }
        for r in rows
    ]


async def inbound_schedule(session: AsyncSession, airport_code: str):
    rows = (await session.execute(SQL_INBOUND, {"arrival_airport": airport_code})).mappings().all()
    return [
        {
            "flightNo": r["route_no"],
            "daysOfWeek": r["days_of_week"],
            "arrivalTime": str(r["arrival_time"]),
            "origin": {
                "airportCode": r["origin_airport"],
                "airportName": r["origin_name"],
                "city": r["origin_city"],
                "country": r["origin_country"],
            },
        }
        for r in rows
    ]


async def outbound_schedule(session: AsyncSession, airport_code: str):
    rows = (await session.execute(SQL_OUTBOUND, {"departure_airport": airport_code})).mappings().all()
    return [
        {
            "flightNo": r["route_no"],
            "daysOfWeek": r["days_of_week"],
            "departureTime": str(r["departure_time"]),
            "destination": {
                "airportCode": r["destination_airport"],
                "airportName": r["destination_name"],
                "city": r["destination_city"],
                "country": r["destination_country"],
            },
        }
        for r in rows
    ]