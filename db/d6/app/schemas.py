from datetime import datetime
from enum import Enum
from typing import List, Optional

from pydantic import BaseModel


class CityOut(BaseModel):
    city: str
    country: str


class AirportOut(BaseModel):
    airportCode: str
    airportName: str
    city: str
    country: str


class InboundFlightOut(BaseModel):
    flightNo: str
    daysOfWeek: List[int]
    arrivalTime: str
    origin: AirportOut


class OutboundFlightOut(BaseModel):
    flightNo: str
    daysOfWeek: List[int]
    departureTime: str
    destination: AirportOut


class BookingClass(str, Enum):
    Economy = "Economy"
    Comfort = "Comfort"
    Business = "Business"


class SegmentOut(BaseModel):
    flightId: int
    flightNo: str
    departureAirport: str
    arrivalAirport: str
    departureTime: datetime
    arrivalTime: datetime
    price: float


class RouteSearchResultOut(BaseModel):
    connectionsCount: int
    segments: List[SegmentOut]
    totalPrice: float


class BookingRequest(BaseModel):
    passengerId: str
    passengerName: str
    flightIds: List[int]
    bookingClass: BookingClass


class BookingResponse(BaseModel):
    bookRef: str
    ticketNo: str
    totalAmount: float


class CheckInRequest(BaseModel):
    ticketNo: str
    flightId: int
    seatPreference: Optional[str] = None


class BoardingPassOut(BaseModel):
    ticketNo: str
    flightId: int
    seatNo: str
    boardingNo: int
    boardingTime: datetime


class ErrorOut(BaseModel):
    message: str