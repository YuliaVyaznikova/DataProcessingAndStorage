from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.ext.asyncio import AsyncSession

from app.db import get_session
from app.schemas import AirportOut, CityOut, InboundFlightOut, OutboundFlightOut
from app.crud.airports import (
    list_cities,
    list_airports,
    airports_in_city,
    inbound_schedule,
    outbound_schedule,
)

router = APIRouter(tags=["airports"])


@router.get("/cities", response_model=list[CityOut], summary="list all available cities")
async def get_cities(
    q: str | None = Query(None, description="substring search for city name"),
    session: AsyncSession = Depends(get_session),
):
    return await list_cities(session, search=q)


@router.get("/airports", response_model=list[AirportOut], summary="list all available airports")
async def get_airports(
    q: str | None = Query(None, description="substring search for airport name, city or code"),
    session: AsyncSession = Depends(get_session),
):
    return await list_airports(session, search=q)


@router.get("/cities/airports", response_model=list[AirportOut], summary="list airports within a city")
async def get_city_airports(
    city: str = Query(...),
    country: str | None = Query(None),
    session: AsyncSession = Depends(get_session),
):
    result = await airports_in_city(session, city=city, country=country)
    if not result:
        raise HTTPException(status_code=404, detail="No airports found for this city")
    return result


@router.get("/airports/{airportCode}/schedule/inbound", response_model=list[InboundFlightOut], summary="list inbound schedule for an airport")
async def get_inbound_schedule(
    airportCode: str,
    session: AsyncSession = Depends(get_session),
):
    result = await inbound_schedule(session, airport_code=airportCode)
    if not result:
        raise HTTPException(status_code=404, detail="No inbound schedule found for this airport")
    return result


@router.get("/airports/{airportCode}/schedule/outbound", response_model=list[OutboundFlightOut], summary="list outbound schedule for an airport")
async def get_outbound_schedule(
    airportCode: str,
    session: AsyncSession = Depends(get_session),
):
    result = await outbound_schedule(session, airport_code=airportCode)
    if not result:
        raise HTTPException(status_code=404, detail="No outbound schedule found for this airport")
    return result