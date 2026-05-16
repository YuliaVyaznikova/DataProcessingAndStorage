from fastapi import APIRouter, Body, Depends, HTTPException, Path
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.exc import DBAPIError

from app.db import get_session
from app.schemas import CheckInRequest, BoardingPassOut
from app.crud.checkin import check_in

router = APIRouter(tags=["checkin"])


@router.patch("/bookings/{bookRef}/check-in", response_model=BoardingPassOut, summary="online check-in for a flight")
async def check_in_endpoint(
    bookRef: str = Path(...),
    request: CheckInRequest = Body(...),
    session: AsyncSession = Depends(get_session),
):
    try:
        result = await check_in(
            session,
            ticket_no=request.ticketNo,
            flight_id=request.flightId,
            seat_preference=request.seatPreference,
            book_ref=bookRef,
        )
    except DBAPIError as e:
        if "could not serialize access" in str(e) or "40001" in str(e):
            raise HTTPException(status_code=409, detail="Concurrent check-in conflict, please retry")
        raise HTTPException(status_code=500, detail=str(e))

    if result is None:
        raise HTTPException(status_code=404, detail="segment or flight not found")
    if result == "already_checked_in":
        raise HTTPException(status_code=409, detail="already checked in for this flight")
    if result == "wrong_booking":
        raise HTTPException(status_code=400, detail="ticket does not belong to this booking")

    return result