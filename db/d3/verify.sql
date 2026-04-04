SELECT 'Total people:' AS check_name, COUNT(*) AS count FROM main.person;

SELECT 'People with known gender:' AS check_name, COUNT(*) AS count 
FROM main.person WHERE gender IS NOT NULL;

SELECT 'Males:' AS check_name, COUNT(*) AS count FROM main.person WHERE gender = 'M'
UNION ALL
SELECT 'Females:', COUNT(*) FROM main.person WHERE gender = 'F';

SELECT 'People with spouses:' AS check_name, COUNT(*) AS count 
FROM main.person WHERE spouse_id IS NOT NULL;

SELECT 'People with known father:' AS check_name, COUNT(*) AS count 
FROM main.person WHERE father_id IS NOT NULL;

SELECT 'People with known mother:' AS check_name, COUNT(*) AS count 
FROM main.person WHERE mother_id IS NOT NULL;

SELECT 'Errors: father not male' AS check_name, COUNT(*) AS errors
FROM main.person p
JOIN main.person f ON p.father_id = f.id
WHERE f.gender != 'M';

SELECT 'Errors: mother not female' AS check_name, COUNT(*) AS errors
FROM main.person p
JOIN main.person m ON p.mother_id = m.id
WHERE m.gender != 'F';

SELECT 'Errors: spouses same gender' AS check_name, COUNT(*) AS errors
FROM main.person p1
JOIN main.person p2 ON p1.spouse_id = p2.id
WHERE p1.gender = p2.gender;

SELECT 'Errors: self-reference spouse' AS check_name, COUNT(*) AS errors FROM main.person WHERE id = spouse_id
UNION ALL
SELECT 'Errors: self-reference father', COUNT(*) FROM main.person WHERE id = father_id
UNION ALL
SELECT 'Errors: self-reference mother', COUNT(*) FROM main.person WHERE id = mother_id;

SELECT 'Sample people:' AS info;
SELECT id, first_name, last_name, gender, spouse_id, father_id, mother_id 
FROM main.person LIMIT 10;

SELECT 'Sample siblings:' AS info;
SELECT * FROM main.sibling_view LIMIT 10;

SELECT 'Sibling pairs:' AS check_name, COUNT(*) AS count FROM main.sibling_view;

SELECT 'Summary' AS summary;
SELECT 
    (SELECT COUNT(*) FROM main.person) AS total_people,
    (SELECT COUNT(*) FROM main.person WHERE gender IS NOT NULL) AS with_gender,
    (SELECT COUNT(*) FROM main.person WHERE spouse_id IS NOT NULL) AS with_spouse,
    (SELECT COUNT(*) FROM main.person WHERE father_id IS NOT NULL) AS with_father,
    (SELECT COUNT(*) FROM main.person WHERE mother_id IS NOT NULL) AS with_mother,
    (SELECT COUNT(*) FROM main.sibling_view) AS sibling_pairs;