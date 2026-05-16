CREATE INDEX IF NOT EXISTS idx_flights_route_no_scheduled_departure
    ON bookings.flights (route_no, scheduled_departure);

CREATE INDEX IF NOT EXISTS idx_flights_status
    ON bookings.flights (status)
    WHERE status = 'Scheduled';

CREATE INDEX IF NOT EXISTS idx_routes_departure_airport
    ON bookings.routes (departure_airport);
CREATE INDEX IF NOT EXISTS idx_routes_arrival_airport
    ON bookings.routes (arrival_airport);

CREATE INDEX IF NOT EXISTS idx_segments_ticket_flight
    ON bookings.segments (ticket_no, flight_id);

CREATE INDEX IF NOT EXISTS idx_boarding_passes_ticket_flight
    ON bookings.boarding_passes (ticket_no, flight_id);

CREATE INDEX IF NOT EXISTS idx_boarding_passes_flight_seat
    ON bookings.boarding_passes (flight_id, seat_no);

CREATE INDEX IF NOT EXISTS idx_seats_airplane_fare
    ON bookings.seats (airplane_code, fare_conditions);

CREATE INDEX IF NOT EXISTS idx_tickets_book_ref
    ON bookings.tickets (book_ref);

CREATE INDEX IF NOT EXISTS idx_pricing_rules_lookup
    ON bookings.pricing_rules (route_no, fare_conditions, departure_month, day_of_week);

CREATE INDEX IF NOT EXISTS idx_airports_data_city_trgm
    ON bookings.airports_data USING GIN ((city->>'en') gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_airports_data_name_trgm
    ON bookings.airports_data USING GIN ((airport_name->>'en') gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_segments_flight_fare
    ON bookings.segments (flight_id, fare_conditions);