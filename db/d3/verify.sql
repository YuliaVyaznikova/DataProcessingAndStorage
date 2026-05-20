SELECT 'Total people:' AS check_name, COUNT(*) AS count FROM main.person;

SELECT 'People with known gender:' AS check_name, COUNT(*) AS count 
FROM main.person WHERE gender IS NOT NULL;

SELECT 'Males:' AS check_name, COUNT(*) AS count FROM main.person WHERE gender = 'M'
UNION ALL
SELECT 'Females:', COUNT(*) FROM main.person WHERE gender = 'F'
UNION ALL
SELECT 'Unknown:', COUNT(*) FROM main.person WHERE gender = 'U';

SELECT 'People with spouses:' AS check_name, COUNT(*) AS count 
FROM main.person WHERE spouse_id IS NOT NULL;

SELECT 'Parent link entries:' AS check_name, COUNT(*) AS count FROM main.parent_link;

SELECT 'Father entries:' AS check_name, COUNT(*) AS count 
FROM main.parent_link WHERE parent_role = 'F';

SELECT 'Mother entries:' AS check_name, COUNT(*) AS count 
FROM main.parent_link WHERE parent_role = 'M';

SELECT 'Sibling link entries:' AS check_name, COUNT(*) AS count FROM main.sibling_link;

SELECT 'Brother entries:' AS check_name, COUNT(*) AS count 
FROM main.sibling_link WHERE sibling_type = 'B';

SELECT 'Sister entries:' AS check_name, COUNT(*) AS count 
FROM main.sibling_link WHERE sibling_type = 'S';

SELECT 'Errors: father not male' AS check_name, COUNT(*) AS errors
FROM main.parent_link pl
JOIN main.person p ON pl.parent_id = p.id
WHERE pl.parent_role = 'F' AND p.gender NOT IN ('M', 'U');

SELECT 'Errors: mother not female' AS check_name, COUNT(*) AS errors
FROM main.parent_link pl
JOIN main.person p ON pl.parent_id = p.id
WHERE pl.parent_role = 'M' AND p.gender NOT IN ('F', 'U');

SELECT 'Errors: spouses same gender' AS check_name, COUNT(*) AS errors
FROM main.person p1
JOIN main.person p2 ON p1.spouse_id = p2.id
WHERE p1.gender = p2.gender AND p1.gender IS NOT NULL AND p2.gender IS NOT NULL;

SELECT 'Errors: self-reference' AS check_name, COUNT(*) AS errors FROM main.person WHERE id = spouse_id
UNION ALL
SELECT 'Errors: self-reference parent_link', COUNT(*) FROM main.parent_link WHERE child_id = parent_id
UNION ALL
SELECT 'Errors: self-reference sibling_link', COUNT(*) FROM main.sibling_link WHERE person_id = sibling_id;

SELECT 'Sample people:' AS info;
SELECT id, first_name, last_name, gender, spouse_id
FROM main.person LIMIT 10;

SELECT 'Sample parent links:' AS info;
SELECT * FROM main.parent_link LIMIT 10;

SELECT 'Sample sibling links:' AS info;
SELECT * FROM main.sibling_link LIMIT 10;

SELECT 'Sibling pairs (view):' AS check_name, COUNT(*) AS count FROM main.sibling_view;

SELECT 'Summary' AS summary;
SELECT 
    (SELECT COUNT(*) FROM main.person) AS total_people,
    (SELECT COUNT(*) FROM main.person WHERE gender IS NOT NULL) AS with_gender,
    (SELECT COUNT(*) FROM main.person WHERE spouse_id IS NOT NULL) AS with_spouse,
    (SELECT COUNT(*) FROM main.parent_link) AS parent_links,
    (SELECT COUNT(*) FROM main.parent_link WHERE parent_role = 'F') AS fathers,
    (SELECT COUNT(*) FROM main.parent_link WHERE parent_role = 'M') AS mothers,
    (SELECT COUNT(*) FROM main.sibling_link) AS sibling_links,
    (SELECT COUNT(*) FROM main.sibling_view) AS sibling_pairs;