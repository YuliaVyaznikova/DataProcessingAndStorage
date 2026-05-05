from fastapi import FastAPI
from fastapi.responses import RedirectResponse

from app.routers.airports import router as airports_router
from app.routers.routes import router as routes_router
from app.routers.bookings import router as bookings_router
from app.routers.checkin import router as checkin_router

app = FastAPI(
    title="Flights API",
    version="1.0.0",
    description="RESTful API for flight booking system",
)


@app.get("/", summary="root")
async def root():
    return RedirectResponse(url="/docs")


app.include_router(airports_router, prefix="/api/v1")
app.include_router(routes_router, prefix="/api/v1")
app.include_router(bookings_router, prefix="/api/v1")
app.include_router(checkin_router, prefix="/api/v1")