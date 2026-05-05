from fastapi import APIRouter, Depends, HTTPException, Path
from sqlalchemy.ext.asyncio import AsyncSession

from app.db import get_session
from app.schemas import CheckInRequest, BoardingPassOut
from app.crud.checkin import check_in

router = APIRouter(tags=["checkin"])


@router.patch("/bookings/{bookRef}/check-in", response_model=BoardingPassOut, summary="online check-in for a flight")
async def check_in_endpoint(
    bookRef: str = Path(...),
    request: CheckInRequest = None,
    session: AsyncSession = Depends(get_session),
):
    result = await check_in(
        session,
        ticket_no=request.ticketNo,
        flight_id=request.flightId,
        seat_preference=request.seatPreference,
        book_ref=bookRef,
    )

    if result is None:
        raise HTTPException(status_code=404, detail="segment or flight not found")
    if result == "already_checked_in":
        raise HTTPException(status_code=409, detail="already checked in for this flight")
    if result == "wrong_booking":
        raise HTTPException(status_code=400, detail="ticket does not belong to this booking")

    return result