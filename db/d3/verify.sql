SELECT 'Общее количество людей:' AS check_name, COUNT(*) AS count FROM main.person;

SELECT 'Людей с известным полом:' AS check_name, COUNT(*) AS count 
FROM main.person WHERE gender IS NOT NULL;

SELECT 'Мужчин:' AS check_name, COUNT(*) AS count FROM main.person WHERE gender = 'M'
UNION ALL
SELECT 'Женщин:', COUNT(*) FROM main.person WHERE gender = 'F';

SELECT 'Людей с супругами:' AS check_name, COUNT(*) AS count 
FROM main.person WHERE spouse_id IS NOT NULL;

SELECT 'Людей с известным отцом:' AS check_name, COUNT(*) AS count 
FROM main.person WHERE father_id IS NOT NULL;

SELECT 'Людей с известной матерью:' AS check_name, COUNT(*) AS count 
FROM main.person WHERE mother_id IS NOT NULL;

SELECT 'Ошибки: отец не male' AS check_name, COUNT(*) AS errors
FROM main.person p
JOIN main.person f ON p.father_id = f.id
WHERE f.gender != 'M';

SELECT 'Ошибки: мать не female' AS check_name, COUNT(*) AS errors
FROM main.person p
JOIN main.person m ON p.mother_id = m.id
WHERE m.gender != 'F';

SELECT 'Ошибки: супруги одного пола' AS check_name, COUNT(*) AS errors
FROM main.person p1
JOIN main.person p2 ON p1.spouse_id = p2.id
WHERE p1.gender = p2.gender;

SELECT 'Ошибки: самоссылки spouse' AS check_name, COUNT(*) AS errors
FROM main.person WHERE id = spouse_id
UNION ALL
SELECT 'Ошибки: самоссылки father', COUNT(*) FROM main.person WHERE id = father_id
UNION ALL
SELECT 'Ошибки: самоссылки mother', COUNT(*) FROM main.person WHERE id = mother_id;

SELECT 'Примеры людей:' AS info;
SELECT id, first_name, last_name, gender, spouse_id, father_id, mother_id 
FROM main.person LIMIT 10;

SELECT 'Примеры сиблингов:' AS info;
SELECT * FROM sibling_view LIMIT 10;

SELECT 'Пар сиблингов:' AS check_name, COUNT(*) AS count FROM sibling_view;

SELECT 'Итого' AS summary;
SELECT 
    (SELECT COUNT(*) FROM main.person) AS total_people,
    (SELECT COUNT(*) FROM main.person WHERE gender IS NOT NULL) AS with_gender,
    (SELECT COUNT(*) FROM main.person WHERE spouse_id IS NOT NULL) AS with_spouse,
    (SELECT COUNT(*) FROM main.person WHERE father_id IS NOT NULL) AS with_father,
    (SELECT COUNT(*) FROM main.person WHERE mother_id IS NOT NULL) AS with_mother,
    (SELECT COUNT(*) FROM sibling_view) AS sibling_pairs;