-- Manual catalog seed: run only against an isolated development or staging database.
-- It is intentionally not a Flyway migration and never changes schema or deletes rows.
BEGIN;

WITH ingredient_seed(name, category, unit, allergens) AS (
  SELECT format('Nguyên liệu Việt Nam %s', lpad(n::text, 3, '0')),
         CASE n % 5 WHEN 0 THEN 'protein' WHEN 1 THEN 'vegetable' WHEN 2 THEN 'carbohydrate' WHEN 3 THEN 'fruit' ELSE 'seasoning' END,
         'g', ''
  FROM generate_series(1, 300) n
), named_allergens(name, allergens) AS (
  VALUES ('Nguyên liệu Việt Nam 001', 'EGG'), ('Nguyên liệu Việt Nam 002', 'FISH'),
         ('Nguyên liệu Việt Nam 003', 'SHELLFISH'), ('Nguyên liệu Việt Nam 004', 'SOY'),
         ('Nguyên liệu Việt Nam 005', 'MILK'), ('Nguyên liệu Việt Nam 006', 'WHEAT'),
         ('Nguyên liệu Việt Nam 007', 'PEANUT'), ('Nguyên liệu Việt Nam 008', 'SESAME')
)
INSERT INTO ingredients (id, created_at, is_deleted, name, normalized_name, category, unit,
  calories_per_unit, protein_per_unit, fat_per_unit, carb_per_unit, default_quantity, allergens, gradient_from, gradient_to)
SELECT ('a' || substr(md5('manual-catalog:' || lower(s.name)), 2, 31))::uuid, now(), false,
  s.name, lower(s.name), s.category, s.unit, 1.0000, .0500, .0200, .1200, 100.0000,
  coalesce(a.allergens, s.allergens), '#EF9D1F', '#FFF0DE'
FROM ingredient_seed s LEFT JOIN named_allergens a ON a.name=s.name
WHERE NOT EXISTS (SELECT 1 FROM ingredients i WHERE i.normalized_name=lower(s.name) AND NOT i.is_deleted);

WITH recipe_seed(name, cooking_time_minutes, difficulty) AS (
  SELECT format('Món ăn Việt Nam %s', lpad(n::text, 3, '0')),
         15 + (n % 4) * 10, CASE n % 3 WHEN 0 THEN 'easy' WHEN 1 THEN 'medium' ELSE 'hard' END
  FROM generate_series(1, 150) n
)
INSERT INTO recipes (id, created_at, is_deleted, name, description, allergens, cooking_time_minutes,
  difficulty, serving_size, instruction_text, source_type, gradient_from, gradient_to)
SELECT ('b' || substr(md5('manual-recipe:' || lower(name)), 2, 31))::uuid, now(), false, name,
  'Món ăn trong catalog seed; cần biên tập lại công thức và dinh dưỡng trước khi dùng production.', '',
  cooking_time_minutes, difficulty, 2, 'Chuẩn bị nguyên liệu, chế biến chín kỹ và kiểm tra cảnh báo dị ứng.',
  'manual-catalog-seed', '#EF9D1F', '#FFF0DE'
FROM recipe_seed s WHERE NOT EXISTS (SELECT 1 FROM recipes r WHERE r.name=s.name AND NOT r.is_deleted);

WITH links AS (
  SELECT r.id recipe_id, i.id ingredient_id, row_number() over (partition by r.id) position
  FROM recipes r JOIN LATERAL (
    SELECT id FROM ingredients WHERE NOT is_deleted ORDER BY normalized_name
    OFFSET ((abs(hashtext(r.name)) % 280)) LIMIT 5
  ) i ON true
  WHERE r.source_type='manual-catalog-seed' AND NOT r.is_deleted
)
INSERT INTO recipe_ingredients (id, created_at, is_deleted, recipe_id, ingredient_id, quantity, unit, is_required, note)
SELECT (substr(md5(recipe_id::text || ':' || ingredient_id::text),1,8)||'-'||substr(md5(recipe_id::text || ':' || ingredient_id::text),9,4)||'-'||substr(md5(recipe_id::text || ':' || ingredient_id::text),13,4)||'-'||substr(md5(recipe_id::text || ':' || ingredient_id::text),17,4)||'-'||substr(md5(recipe_id::text || ':' || ingredient_id::text),21,12))::uuid,
  now(), false, recipe_id, ingredient_id, CASE position WHEN 1 THEN 150 ELSE 50 END, 'g', true, NULL
FROM links l WHERE NOT EXISTS (SELECT 1 FROM recipe_ingredients ri WHERE ri.recipe_id=l.recipe_id AND ri.ingredient_id=l.ingredient_id AND NOT ri.is_deleted);

UPDATE recipes r SET allergens=coalesce(a.allergens, ''), updated_at=now()
FROM (
  SELECT r.id, string_agg(DISTINCT x.allergen, ',' ORDER BY x.allergen) allergens
  FROM recipes r JOIN recipe_ingredients ri ON ri.recipe_id=r.id AND NOT ri.is_deleted
  JOIN ingredients i ON i.id=ri.ingredient_id AND NOT i.is_deleted
  CROSS JOIN LATERAL unnest(string_to_array(nullif(i.allergens,''), ',')) x(allergen)
  WHERE r.source_type='manual-catalog-seed' AND NOT r.is_deleted GROUP BY r.id
) a WHERE a.id=r.id;

COMMIT;

SELECT count(*) AS seeded_recipes FROM recipes WHERE source_type='manual-catalog-seed' AND NOT is_deleted;
SELECT r.name, r.allergens FROM recipes r WHERE r.source_type='manual-catalog-seed' AND NOT r.is_deleted ORDER BY r.name;
