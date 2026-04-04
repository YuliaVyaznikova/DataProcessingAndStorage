DROP TABLE IF EXISTS bookings.pricing_rules;

CREATE TABLE bookings.pricing_rules AS
SELECT 
    f.route_no,
    s.fare_conditions,
    EXTRACT(MONTH FROM f.scheduled_departure) AS departure_month,
    EXTRACT(ISODOW FROM f.scheduled_departure) AS day_of_week,
    CASE 
        WHEN EXTRACT(DAY FROM (f.scheduled_departure - b.book_date)) > 21 THEN 'Early'
        WHEN EXTRACT(DAY FROM (f.scheduled_departure - b.book_date)) BETWEEN 7 AND 21 THEN 'Standard'
        ELSE 'Late'
    END AS booking_category,
    percentile_cont(0.5) WITHIN GROUP (ORDER BY s.price) AS base_price,
    MIN(s.price) AS min_price,
    MAX(s.price) AS max_price,
    COUNT(*) AS sales_count
FROM bookings.segments s
JOIN bookings.flights f ON s.flight_id = f.flight_id
JOIN bookings.routes r ON f.route_no = r.route_no
JOIN bookings.tickets t ON s.ticket_no = t.ticket_no
JOIN bookings.bookings b ON t.book_ref = b.book_ref
WHERE f.status = 'Arrived'
GROUP BY 1, 2, 3, 4, 5
HAVING COUNT(*) >= 3;

SELECT COUNT(*) AS total_rules FROM bookings.pricing_rules;

SELECT * FROM bookings.pricing_rules LIMIT 5;

SELECT fare_conditions, booking_category, COUNT(*) AS cnt, ROUND(AVG(base_price)::numeric, 2) AS avg_price
FROM bookings.pricing_rules
GROUP BY fare_conditions, booking_category
ORDER BY fare_conditions, booking_category;