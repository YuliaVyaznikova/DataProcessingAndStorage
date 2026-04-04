DROP TABLE IF EXISTS bookings.pricing_rules CASCADE;

CREATE TABLE bookings.pricing_rules (
    route_no            TEXT NOT NULL,
    fare_conditions     TEXT NOT NULL CHECK (fare_conditions IN ('Economy', 'Comfort', 'Business')),
    departure_month     INTEGER NOT NULL CHECK (departure_month BETWEEN 1 AND 12),
    day_of_week         INTEGER NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    booking_category    TEXT NOT NULL CHECK (booking_category IN ('Early', 'Standard', 'Late')),
    avg_duration_minutes NUMERIC(10,2),
    base_price          NUMERIC(10,2) NOT NULL CHECK (base_price >= 0),
    min_price           NUMERIC(10,2) NOT NULL,
    max_price           NUMERIC(10,2) NOT NULL,
    sales_count         INTEGER NOT NULL DEFAULT 0,
    last_updated        TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    PRIMARY KEY (route_no, fare_conditions, departure_month, day_of_week, booking_category)
);

INSERT INTO bookings.pricing_rules 
    (route_no, fare_conditions, departure_month, day_of_week, booking_category, 
     avg_duration_minutes, base_price, min_price, max_price, sales_count)
SELECT 
    f.route_no,
    s.fare_conditions,
    EXTRACT(MONTH FROM f.scheduled_departure)::INTEGER,
    EXTRACT(ISODOW FROM f.scheduled_departure)::INTEGER,
    CASE 
        WHEN EXTRACT(DAY FROM (f.scheduled_departure - b.book_date)) > 21 THEN 'Early'
        WHEN EXTRACT(DAY FROM (f.scheduled_departure - b.book_date)) BETWEEN 7 AND 21 THEN 'Standard'
        ELSE 'Late'
    END,
    ROUND(AVG(EXTRACT(EPOCH FROM r.duration) / 60)::numeric, 2),
    ROUND(percentile_cont(0.5) WITHIN GROUP (ORDER BY s.price)::numeric, 2),
    ROUND(MIN(s.price)::numeric, 2),
    ROUND(MAX(s.price)::numeric, 2),
    COUNT(*)
FROM bookings.segments s
JOIN bookings.flights f ON s.flight_id = f.flight_id
JOIN bookings.routes r ON f.route_no = r.route_no
JOIN bookings.tickets t ON s.ticket_no = t.ticket_no
JOIN bookings.bookings b ON t.book_ref = b.book_ref
WHERE f.status = 'Arrived'
GROUP BY 
    f.route_no,
    s.fare_conditions,
    EXTRACT(MONTH FROM f.scheduled_departure)::INTEGER,
    EXTRACT(ISODOW FROM f.scheduled_departure)::INTEGER,
    CASE 
        WHEN EXTRACT(DAY FROM (f.scheduled_departure - b.book_date)) > 21 THEN 'Early'
        WHEN EXTRACT(DAY FROM (f.scheduled_departure - b.book_date)) BETWEEN 7 AND 21 THEN 'Standard'
        ELSE 'Late'
    END
HAVING COUNT(*) >= 3;

CREATE OR REPLACE FUNCTION bookings.get_flight_price(
    p_route_no TEXT,
    p_fare_conditions TEXT,
    p_departure_month INTEGER,
    p_day_of_week INTEGER,
    p_booking_category TEXT
) RETURNS NUMERIC AS $$
DECLARE
    v_price NUMERIC;
BEGIN
    SELECT base_price INTO v_price
    FROM bookings.pricing_rules
    WHERE route_no = p_route_no
      AND fare_conditions = p_fare_conditions
      AND departure_month = p_departure_month
      AND day_of_week = p_day_of_week
      AND booking_category = p_booking_category;
    
    RETURN COALESCE(v_price, 0);
END;
$$ LANGUAGE plpgsql;

SELECT COUNT(*) as total_rules FROM bookings.pricing_rules;

SELECT fare_conditions, booking_category, COUNT(*) as cnt, 
       ROUND(AVG(base_price)::numeric, 2) as avg_base_price,
       ROUND(AVG(min_price)::numeric, 2) as avg_min,
       ROUND(AVG(max_price)::numeric, 2) as avg_max
FROM bookings.pricing_rules
GROUP BY fare_conditions, booking_category
ORDER BY fare_conditions, booking_category;