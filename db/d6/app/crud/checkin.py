from datetime import timedelta

from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncSession


async def check_in(
    session: AsyncSession,
    ticket_no: str,
    flight_id: int,
    seat_preference: str | None = None,
    book_ref: str | None = None,
) -> dict | str | None:
    await session.execute(text("SET TRANSACTION ISOLATION LEVEL SERIALIZABLE"))

    segment_sql = text("""
        SELECT s.fare_conditions
        FROM bookings.segments s
        WHERE s.ticket_no = :ticket_no AND s.flight_id = :flight_id
    """)
    segment = (await session.execute(segment_sql, {
        "ticket_no": ticket_no,
        "flight_id": flight_id,
    })).mappings().one_or_none()

    if not segment:
        return None

    if book_ref is not None:
        ticket_sql = text("""
            SELECT t.book_ref
            FROM bookings.tickets t
            WHERE t.ticket_no = :ticket_no
        """)
        ticket_row = (await session.execute(ticket_sql, {"ticket_no": ticket_no})).scalar_one_or_none()
        if ticket_row != book_ref:
            return "wrong_booking"

    boarding_sql = text("""
        SELECT 1
        FROM bookings.boarding_passes bp
        WHERE bp.ticket_no = :ticket_no AND bp.flight_id = :flight_id
    """)
    existing = (await session.execute(boarding_sql, {
        "ticket_no": ticket_no,
        "flight_id": flight_id,
    })).first()
    if existing:
        return "already_checked_in"

    flight_sql = text("""
        SELECT f.scheduled_departure, r.airplane_code
        FROM bookings.flights f
        JOIN bookings.routes r ON r.route_no = f.route_no
                              AND f.scheduled_departure <@ r.validity
        WHERE f.flight_id = :flight_id
    """)
    flight = (await session.execute(flight_sql, {"flight_id": flight_id})).mappings().one_or_none()

    if not flight:
        return None

    fare = segment["fare_conditions"]
    aircraft = flight["airplane_code"]

    seat_no = None

    if seat_preference:
        pref_sql = text("""
            SELECT s.seat_no
            FROM bookings.seats s
            WHERE s.airplane_code = :airplane_code
              AND s.fare_conditions = :fare_conditions
              AND s.seat_no = :seat_preference
              AND NOT EXISTS (
                  SELECT 1
                  FROM bookings.boarding_passes bp
                  WHERE bp.flight_id = :flight_id
                    AND bp.seat_no = s.seat_no
              )
            LIMIT 1
            FOR UPDATE OF s
        """)
        seat_no = (await session.execute(pref_sql, {
            "airplane_code": aircraft,
            "fare_conditions": fare,
            "seat_preference": seat_preference,
            "flight_id": flight_id,
        })).scalar_one_or_none()

    if not seat_no:
        seat_sql = text("""
            SELECT s.seat_no
            FROM bookings.seats s
            WHERE s.airplane_code = :airplane_code
              AND s.fare_conditions = :fare_conditions
              AND NOT EXISTS (
                  SELECT 1
                  FROM bookings.boarding_passes bp
                  WHERE bp.flight_id = :flight_id
                    AND bp.seat_no = s.seat_no
              )
            ORDER BY s.seat_no
            LIMIT 1
            FOR UPDATE OF s
        """)
        seat_no = (await session.execute(seat_sql, {
            "airplane_code": aircraft,
            "fare_conditions": fare,
            "flight_id": flight_id,
        })).scalar_one_or_none()

    if not seat_no:
        return None

    boarding_no_sql = text("""
        SELECT COALESCE(MAX(bp.boarding_no), 0) + 1
        FROM bookings.boarding_passes bp
        WHERE bp.flight_id = :flight_id
    """)
    boarding_no = (await session.execute(boarding_no_sql, {"flight_id": flight_id})).scalar()

    boarding_time = flight["scheduled_departure"] - timedelta(minutes=30)

    insert_sql = text("""
        INSERT INTO bookings.boarding_passes (ticket_no, flight_id, boarding_no, seat_no, boarding_time)
        VALUES (:ticket_no, :flight_id, :boarding_no, :seat_no, :boarding_time)
    """)
    await session.execute(insert_sql, {
        "ticket_no": ticket_no,
        "flight_id": flight_id,
        "boarding_no": boarding_no,
        "seat_no": seat_no,
        "boarding_time": boarding_time,
    })

    await session.commit()

    return {
        "ticketNo": ticket_no,
        "flightId": flight_id,
        "seatNo": seat_no,
        "boardingNo": boarding_no,
        "boardingTime": boarding_time,
    }
