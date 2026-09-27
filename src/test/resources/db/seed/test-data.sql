-- Synthetic integration-test data only.
-- All seeded users use the TEST-ONLY password documented in
-- contracts/auth/password-hash-v0-vector.properties. Never deploy this file.
INSERT INTO users (id, created_at, full_name, email, password_hashed,
                   is_email_confirmed, is_active, role)
VALUES
('00000000-0000-0000-0000-000000000101', '2026-01-01T00:00:00Z', 'Test Super Admin', 'superadmin@test.local',
 'AIAxJyUGQQCIG+pquXusORUfScFNRqux2b91Uv/6ymchMc0X0P5lIjEpcO9ZMz2Puw==', true, true, 'SUPER_ADMIN'),
('00000000-0000-0000-0000-000000000102', '2026-01-01T00:00:00Z', 'Test Admin', 'admin@test.local',
 'AIAxJyUGQQCIG+pquXusORUfScFNRqux2b91Uv/6ymchMc0X0P5lIjEpcO9ZMz2Puw==', true, true, 'ADMIN'),
('00000000-0000-0000-0000-000000000103', '2026-01-01T00:00:00Z', 'Test Manager', 'manager@test.local',
 'AIAxJyUGQQCIG+pquXusORUfScFNRqux2b91Uv/6ymchMc0X0P5lIjEpcO9ZMz2Puw==', true, true, 'MANAGER'),
('00000000-0000-0000-0000-000000000104', '2026-01-01T00:00:00Z', 'Test User', 'user@test.local',
 'AIAxJyUGQQCIG+pquXusORUfScFNRqux2b91Uv/6ymchMc0X0P5lIjEpcO9ZMz2Puw==', true, true, 'USER'),
('00000000-0000-0000-0000-000000000105', '2026-01-01T00:00:00Z', 'Test Inactive User', 'inactive@test.local',
 'AIAxJyUGQQCIG+pquXusORUfScFNRqux2b91Uv/6ymchMc0X0P5lIjEpcO9ZMz2Puw==', true, false, 'USER');

INSERT INTO ingredients (id, created_at, name, normalized_name, category, unit,
                         calories_per_unit, protein_per_unit, fat_per_unit, carb_per_unit)
VALUES
('00000000-0000-0000-0000-000000000201', '2026-01-01T00:00:00Z', 'Rice', 'rice', 'grain', 'g', 1.3000, 0.0270, 0.0030, 0.2800),
('00000000-0000-0000-0000-000000000202', '2026-01-01T00:00:00Z', 'Egg', 'egg', 'protein', 'item', 78.0000, 6.3000, 5.3000, 0.6000),
('00000000-0000-0000-0000-000000000203', '2026-01-01T00:00:00Z', 'Tomato', 'tomato', 'vegetable', 'g', 0.1800, 0.0090, 0.0020, 0.0390);

INSERT INTO ingredient_aliases (id, created_at, ingredient_id, alias_name, normalized_alias_name)
VALUES
('00000000-0000-0000-0000-000000000211', '2026-01-01T00:00:00Z', '00000000-0000-0000-0000-000000000201', 'White rice', 'white rice'),
('00000000-0000-0000-0000-000000000212', '2026-01-01T00:00:00Z', '00000000-0000-0000-0000-000000000202', 'Chicken egg', 'chicken egg');

INSERT INTO recipes (id, created_at, name, description, cooking_time_minutes, difficulty,
                     serving_size, instruction_text, source_type)
VALUES
('00000000-0000-0000-0000-000000000301', '2026-01-01T00:00:00Z', 'Egg Rice', 'Synthetic recipe', 15, 'easy', 1, 'Cook rice and egg.', 'test'),
('00000000-0000-0000-0000-000000000302', '2026-01-01T00:00:00Z', 'Tomato Egg', 'Synthetic recipe', 10, 'easy', 2, 'Cook tomato and egg.', 'test');

INSERT INTO recipe_ingredients (id, created_at, recipe_id, ingredient_id, quantity, unit, is_required)
VALUES
('00000000-0000-0000-0000-000000000311', '2026-01-01T00:00:00Z', '00000000-0000-0000-0000-000000000301', '00000000-0000-0000-0000-000000000201', 100.0000, 'g', true),
('00000000-0000-0000-0000-000000000312', '2026-01-01T00:00:00Z', '00000000-0000-0000-0000-000000000301', '00000000-0000-0000-0000-000000000202', 1.0000, 'item', true),
('00000000-0000-0000-0000-000000000313', '2026-01-01T00:00:00Z', '00000000-0000-0000-0000-000000000302', '00000000-0000-0000-0000-000000000203', 150.0000, 'g', true),
('00000000-0000-0000-0000-000000000314', '2026-01-01T00:00:00Z', '00000000-0000-0000-0000-000000000302', '00000000-0000-0000-0000-000000000202', 2.0000, 'item', true);

INSERT INTO user_pantry_items (id, created_at, user_id, ingredient_id, quantity, unit, storage_location)
VALUES
('00000000-0000-0000-0000-000000000401', '2026-01-01T00:00:00Z', '00000000-0000-0000-0000-000000000102', '00000000-0000-0000-0000-000000000201', 500.0000, 'g', 'pantry'),
('00000000-0000-0000-0000-000000000402', '2026-01-01T00:00:00Z', '00000000-0000-0000-0000-000000000102', '00000000-0000-0000-0000-000000000202', 6.0000, 'item', 'fridge'),
('00000000-0000-0000-0000-000000000403', '2026-01-01T00:00:00Z', '00000000-0000-0000-0000-000000000103', '00000000-0000-0000-0000-000000000203', 300.0000, 'g', 'fridge');

INSERT INTO today_menu_items (id, created_at, user_id, recipe_id, meal_name, meal_type,
                              serving_size, planned_date, status, note)
VALUES ('00000000-0000-0000-0000-000000000501', '2026-01-01T00:00:00Z',
        '00000000-0000-0000-0000-000000000102', '00000000-0000-0000-0000-000000000301',
        'Test Egg Rice', 'dinner', 1, '2026-01-01', 'Planned', 'Synthetic menu item');

INSERT INTO media_assets (id, created_at, recipe_id, public_id, url, secure_url, resource_type, format, width, height)
VALUES ('00000000-0000-0000-0000-000000000601', '2026-01-01T00:00:00Z',
        '00000000-0000-0000-0000-000000000301', 'synthetic/test-asset',
        'http://example.invalid/test.png', 'https://example.invalid/test.png', 'image', 'png', 1, 1);
