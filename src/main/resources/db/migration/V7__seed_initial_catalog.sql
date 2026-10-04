-- Initial ZPantry catalog for an empty Java-owned database.
-- Flyway applies this once after V1–V6. It inserts only catalog data and never alters schema.

BEGIN;

INSERT INTO ingredients (
    id, created_at, is_deleted, name, normalized_name, category, unit,
    calories_per_unit, protein_per_unit, fat_per_unit, carb_per_unit,
    default_quantity, allergens, gradient_from, gradient_to
)
SELECT seed.id, now(), false, seed.name, seed.normalized_name, seed.category, seed.unit,
       seed.calories, seed.protein, seed.fat, seed.carbs,
       seed.default_quantity, seed.allergens, seed.gradient_from, seed.gradient_to
FROM (VALUES
    ('10000000-0000-0000-0000-000000000001'::uuid, 'Trứng gà',       'trứng gà',       'protein',      'quả', 72.0000,  6.3000,  4.8000,  0.4000, 1.0000,   'EGG',                  '#F59E0B', '#FDE68A'),
    ('10000000-0000-0000-0000-000000000002'::uuid, 'Thịt ức gà',    'thịt ức gà',     'protein',      'g',  1.6500,  0.3100,  0.0360,  0.0000, 100.0000, '',                     '#F97316', '#FED7AA'),
    ('10000000-0000-0000-0000-000000000003'::uuid, 'Cá hồi',        'cá hồi',         'protein',      'g',  2.0800,  0.2000,  0.1300,  0.0000, 100.0000, 'FISH',                 '#FB7185', '#FBCFE8'),
    ('10000000-0000-0000-0000-000000000004'::uuid, 'Đậu hũ',        'đậu hũ',         'protein',      'g',  0.7600,  0.0800,  0.0480,  0.0190, 100.0000, 'SOY',                  '#EAB308', '#FEF9C3'),
    ('10000000-0000-0000-0000-000000000005'::uuid, 'Tôm',           'tôm',            'protein',      'g',  0.9900,  0.2400,  0.0030,  0.0020, 100.0000, 'SHELLFISH',            '#F43F5E', '#FFE4E6'),
    ('10000000-0000-0000-0000-000000000006'::uuid, 'Cơm trắng',     'cơm trắng',      'carbohydrate', 'g',  1.3000,  0.0270,  0.0030,  0.2820, 100.0000, '',                     '#FBBF24', '#FEF3C7'),
    ('10000000-0000-0000-0000-000000000007'::uuid, 'Khoai lang',    'khoai lang',     'carbohydrate', 'g',  0.8600,  0.0160,  0.0010,  0.2010, 100.0000, '',                     '#FB923C', '#FFEDD5'),
    ('10000000-0000-0000-0000-000000000008'::uuid, 'Cà chua',       'cà chua',        'vegetable',    'g',  0.1800,  0.0090,  0.0020,  0.0390, 100.0000, '',                     '#EF4444', '#FEE2E2'),
    ('10000000-0000-0000-0000-000000000009'::uuid, 'Rau cải xanh',  'rau cải xanh',   'vegetable',    'g',  0.2700,  0.0270,  0.0020,  0.0420, 100.0000, '',                     '#22C55E', '#DCFCE7'),
    ('10000000-0000-0000-0000-000000000010'::uuid, 'Dưa leo',       'dưa leo',        'vegetable',    'g',  0.1500,  0.0070,  0.0010,  0.0360, 100.0000, '',                     '#10B981', '#D1FAE5'),
    ('10000000-0000-0000-0000-000000000011'::uuid, 'Hành tây',      'hành tây',       'vegetable',    'g',  0.4000,  0.0110,  0.0010,  0.0930, 100.0000, '',                     '#A855F7', '#F3E8FF'),
    ('10000000-0000-0000-0000-000000000012'::uuid, 'Tỏi',           'tỏi',            'seasoning',    'g',  1.4900,  0.0640,  0.0050,  0.3310, 100.0000, '',                     '#E5E7EB', '#FFFFFF'),
    ('10000000-0000-0000-0000-000000000013'::uuid, 'Dầu ăn',        'dầu ăn',         'seasoning',    'ml', 8.8400,  0.0000,  1.0000,  0.0000, 100.0000, '',                     '#EAB308', '#FEF08A'),
    ('10000000-0000-0000-0000-000000000014'::uuid, 'Nước mắm',      'nước mắm',       'seasoning',    'ml', 0.3500,  0.0550,  0.0000,  0.0320, 100.0000, 'FISH',                 '#92400E', '#FDE68A'),
    ('10000000-0000-0000-0000-000000000015'::uuid, 'Bún tươi',      'bún tươi',       'carbohydrate', 'g',  1.0900,  0.0180,  0.0020,  0.2490, 100.0000, 'WHEAT',                '#94A3B8', '#F8FAFC')
) AS seed(id, name, normalized_name, category, unit, calories, protein, fat, carbs,
          default_quantity, allergens, gradient_from, gradient_to)
WHERE NOT EXISTS (
    SELECT 1 FROM ingredients existing
    WHERE existing.normalized_name = seed.normalized_name AND existing.is_deleted = false
);

INSERT INTO recipes (
    id, created_at, is_deleted, name, description, allergens, cooking_time_minutes,
    difficulty, serving_size, instruction_text, source_type, gradient_from, gradient_to
)
SELECT seed.id, now(), false, seed.name, seed.description, seed.allergens, seed.cooking_time_minutes,
       seed.difficulty, seed.serving_size, seed.instruction_text, 'sql-development-seed',
       seed.gradient_from, seed.gradient_to
FROM (VALUES
    ('20000000-0000-0000-0000-000000000001'::uuid, 'Cơm gà rau cải', 'Bữa cơm giàu đạm, chế biến nhanh.', '', 25, 'easy', 1, 'Áp chảo gà với tỏi. Luộc rau cải và dùng cùng cơm trắng.', '#F97316', '#FED7AA'),
    ('20000000-0000-0000-0000-000000000002'::uuid, 'Trứng chiên cà chua', 'Món trứng đơn giản cho bữa sáng.', 'EGG', 15, 'easy', 1, 'Xào hành tây và cà chua. Đổ trứng đã đánh vào, chiên đến khi chín.', '#EF4444', '#FEE2E2'),
    ('20000000-0000-0000-0000-000000000003'::uuid, 'Đậu hũ sốt cà chua', 'Món chay giàu đạm thực vật.', 'SOY', 20, 'easy', 2, 'Áp chảo đậu hũ. Nấu cà chua, hành tây và tỏi thành sốt rồi cho đậu hũ vào.', '#EAB308', '#FEF9C3'),
    ('20000000-0000-0000-0000-000000000004'::uuid, 'Cá hồi áp chảo khoai lang', 'Bữa ăn cân bằng với cá hồi và khoai lang.', 'FISH', 30, 'medium', 1, 'Nướng khoai lang. Áp chảo cá hồi với tỏi, dùng kèm dưa leo.', '#FB7185', '#FBCFE8'),
    ('20000000-0000-0000-0000-000000000005'::uuid, 'Bún tôm rau cải', 'Bún tươi với tôm và rau xanh.', 'SHELLFISH,WHEAT,FISH', 25, 'medium', 1, 'Luộc bún. Xào tôm với tỏi, thêm rau cải và nêm nước mắm.', '#38BDF8', '#E0F2FE')
) AS seed(id, name, description, allergens, cooking_time_minutes, difficulty, serving_size,
          instruction_text, gradient_from, gradient_to)
WHERE NOT EXISTS (
    SELECT 1 FROM recipes existing
    WHERE existing.name = seed.name AND existing.is_deleted = false
);

WITH seed(recipe_name, ingredient_normalized_name, quantity, unit, note) AS (
    VALUES
        ('Cơm gà rau cải', 'thịt ức gà', 150.0000, 'g', NULL),
        ('Cơm gà rau cải', 'rau cải xanh', 150.0000, 'g', NULL),
        ('Cơm gà rau cải', 'cơm trắng', 200.0000, 'g', NULL),
        ('Cơm gà rau cải', 'tỏi', 5.0000, 'g', NULL),
        ('Trứng chiên cà chua', 'trứng gà', 2.0000, 'quả', NULL),
        ('Trứng chiên cà chua', 'cà chua', 150.0000, 'g', NULL),
        ('Trứng chiên cà chua', 'hành tây', 50.0000, 'g', NULL),
        ('Trứng chiên cà chua', 'dầu ăn', 10.0000, 'ml', NULL),
        ('Đậu hũ sốt cà chua', 'đậu hũ', 300.0000, 'g', NULL),
        ('Đậu hũ sốt cà chua', 'cà chua', 200.0000, 'g', NULL),
        ('Đậu hũ sốt cà chua', 'hành tây', 50.0000, 'g', NULL),
        ('Đậu hũ sốt cà chua', 'tỏi', 5.0000, 'g', NULL),
        ('Cá hồi áp chảo khoai lang', 'cá hồi', 180.0000, 'g', NULL),
        ('Cá hồi áp chảo khoai lang', 'khoai lang', 200.0000, 'g', NULL),
        ('Cá hồi áp chảo khoai lang', 'dưa leo', 100.0000, 'g', NULL),
        ('Cá hồi áp chảo khoai lang', 'tỏi', 5.0000, 'g', NULL),
        ('Bún tôm rau cải', 'bún tươi', 200.0000, 'g', NULL),
        ('Bún tôm rau cải', 'tôm', 150.0000, 'g', NULL),
        ('Bún tôm rau cải', 'rau cải xanh', 100.0000, 'g', NULL),
        ('Bún tôm rau cải', 'tỏi', 5.0000, 'g', NULL),
        ('Bún tôm rau cải', 'nước mắm', 10.0000, 'ml', NULL)
)
INSERT INTO recipe_ingredients (
    id, created_at, is_deleted, recipe_id, ingredient_id, quantity, unit, is_required, note
)
SELECT (
           substr(md5(recipe.id::text || ':' || ingredient.id::text), 1, 8) || '-' ||
           substr(md5(recipe.id::text || ':' || ingredient.id::text), 9, 4) || '-' ||
           substr(md5(recipe.id::text || ':' || ingredient.id::text), 13, 4) || '-' ||
           substr(md5(recipe.id::text || ':' || ingredient.id::text), 17, 4) || '-' ||
           substr(md5(recipe.id::text || ':' || ingredient.id::text), 21, 12)
       )::uuid,
       now(), false, recipe.id, ingredient.id, seed.quantity, seed.unit, true, seed.note
FROM seed
JOIN recipes recipe ON recipe.name = seed.recipe_name AND recipe.is_deleted = false
JOIN ingredients ingredient ON ingredient.normalized_name = seed.ingredient_normalized_name
    AND ingredient.is_deleted = false
WHERE NOT EXISTS (
    SELECT 1 FROM recipe_ingredients existing
    WHERE existing.recipe_id = recipe.id
      AND existing.ingredient_id = ingredient.id
      AND existing.is_deleted = false
);

COMMIT;
