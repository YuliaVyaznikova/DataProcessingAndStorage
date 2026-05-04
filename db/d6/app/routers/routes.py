from datetime import date
from typing import Optional

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.ext.asyncio import AsyncSession

from app.db import get_session
from app.schemas import BookingClass, RouteSearchResultOut
from app.crud.routes import search_routes

router = APIRouter(tags=["routes"])


@router.get("/routes/search", response_model=list[RouteSearchResultOut], summary="search routes connecting two points")
async def search_routes_endpoint(
    fromAirportCode: Optional[str] = Query(None),
    fromCity: Optional[str] = Query(None),
    fromCountry: Optional[str] = Query(None),
    toAirportCode: Optional[str] = Query(None),
    toCity: Optional[str] = Query(None),
    toCountry: Optional[str] = Query(None),
    departureDate: date = Query(...),
    bookingClass: Optional[BookingClass] = Query(None),
    maxConnections: int = Query(-1),
    session: AsyncSession = Depends(get_session),
):
    if fromAirportCode is None and fromCity is None:
        raise HTTPException(status_code=400, detail="Either fromAirportCode or fromCity is required")

    if toAirportCode is None and toCity is None:
        raise HTTPException(status_code=400, detail="Either toAirportCode or toCity is required")

    results = await search_routes(
        session,
        from_airport=fromAirportCode,
        from_city=fromCity,
        from_country=fromCountry,
        to_airport=toAirportCode,
        to_city=toCity,
        to_country=toCountry,
        departure_date=departureDate,
        max_connections=maxConnections,
        booking_class=bookingClass.value if bookingClass else None,
    )

    return results