from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession

from app.db import get_session
from app.schemas import BookingRequest, BookingResponse
from app.crud.bookings import create_booking

router = APIRouter(tags=["bookings"])


@router.post("/bookings", response_model=BookingResponse, status_code=201, summary="create a booking for a selected route")
async def create_booking_endpoint(
    request: BookingRequest,
    session: AsyncSession = Depends(get_session),
):
    try:
        result = await create_booking(
            session,
            passenger_id=request.passengerId,
            passenger_name=request.passengerName,
            flight_ids=request.flightIds,
            booking_class=request.bookingClass.value,
        )
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))

    return result