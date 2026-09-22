-- Insert sample data for users
INSERT INTO users (id, created_at, is_deleted, full_name, email, password_hashed, is_email_confirmed, is_active, role) VALUES
('11111111-1111-1111-1111-111111111111', NOW(), false, 'Admin User', 'admin@zpantry.com', 'hashed_pass_1', true, true, 'admin'),
('22222222-2222-2222-2222-222222222222', NOW(), false, 'Test User 1', 'test1@zpantry.com', 'hashed_pass_2', true, true, 'user'),
('33333333-3333-3333-3333-333333333333', NOW(), false, 'Test User 2', 'test2@zpantry.com', 'hashed_pass_3', false, false, 'user')
ON CONFLICT (id) DO NOTHING;

-- Insert sample data for ingredients
INSERT INTO ingredients (id, created_at, is_deleted, name, normalized_name, category, unit, calories_per_unit, protein_per_unit, fat_per_unit, carb_per_unit) VALUES
('a1111111-1111-1111-1111-111111111111', NOW(), false, 'Tomato', 'tomato', 'Vegetable', 'kg', 180, 9, 2, 39),
('a2222222-2222-2222-2222-222222222222', NOW(), false, 'Chicken Breast', 'chicken_breast', 'Meat', 'kg', 1650, 310, 36, 0),
('a3333333-3333-3333-3333-333333333333', NOW(), false, 'Rice', 'rice', 'Grain', 'kg', 1300, 27, 3, 280)
ON CONFLICT (id) DO NOTHING;

-- Insert sample data for ingredient_aliases
INSERT INTO ingredient_aliases (id, created_at, is_deleted, ingredient_id, alias_name, normalized_alias_name) VALUES
(gen_random_uuid(), NOW(), false, 'a1111111-1111-1111-1111-111111111111', 'Tomatoes', 'tomatoes'),
(gen_random_uuid(), NOW(), false, 'a1111111-1111-1111-1111-111111111111', 'Fresh Tomato', 'fresh_tomato'),
(gen_random_uuid(), NOW(), false, 'a2222222-2222-2222-2222-222222222222', 'Chicken', 'chicken')
ON CONFLICT (id) DO NOTHING;

-- Insert sample data for recipes
INSERT INTO recipes (id, created_at, is_deleted, name, description, cooking_time_minutes, difficulty, serving_size, instruction_text) VALUES
('b1111111-1111-1111-1111-111111111111', NOW(), false, 'Tomato Chicken', 'Delicious tomato chicken', 30, 'Easy', 2, '1. Dice tomato. 2. Cook chicken. 3. Mix together.'),
('b2222222-2222-2222-2222-222222222222', NOW(), false, 'Chicken Rice', 'Classic chicken rice', 45, 'Medium', 4, '1. Boil rice. 2. Grill chicken. 3. Serve together.'),
('b3333333-3333-3333-3333-333333333333', NOW(), false, 'Tomato Rice', 'Vegetarian tomato rice', 20, 'Easy', 2, '1. Cook rice. 2. Blend tomatoes. 3. Mix and simmer.')
ON CONFLICT (id) DO NOTHING;

-- Insert sample data for recipe_ingredients
INSERT INTO recipe_ingredients (id, created_at, is_deleted, recipe_id, ingredient_id, quantity, unit, is_required) VALUES
(gen_random_uuid(), NOW(), false, 'b1111111-1111-1111-1111-111111111111', 'a1111111-1111-1111-1111-111111111111', 0.5, 'kg', true),
(gen_random_uuid(), NOW(), false, 'b1111111-1111-1111-1111-111111111111', 'a2222222-2222-2222-2222-222222222222', 0.5, 'kg', true),
(gen_random_uuid(), NOW(), false, 'b2222222-2222-2222-2222-222222222222', 'a2222222-2222-2222-2222-222222222222', 1.0, 'kg', true),
(gen_random_uuid(), NOW(), false, 'b2222222-2222-2222-2222-222222222222', 'a3333333-3333-3333-3333-333333333333', 0.5, 'kg', true)
ON CONFLICT (id) DO NOTHING;

-- Insert sample data for user_pantry_items
INSERT INTO user_pantry_items (id, created_at, is_deleted, user_id, ingredient_id, quantity, unit, storage_location) VALUES
(gen_random_uuid(), NOW(), false, '22222222-2222-2222-2222-222222222222', 'a1111111-1111-1111-1111-111111111111', 1.0, 'kg', 'Fridge'),
(gen_random_uuid(), NOW(), false, '22222222-2222-2222-2222-222222222222', 'a2222222-2222-2222-2222-222222222222', 2.0, 'kg', 'Freezer'),
(gen_random_uuid(), NOW(), false, '33333333-3333-3333-3333-333333333333', 'a3333333-3333-3333-3333-333333333333', 5.0, 'kg', 'Pantry')
ON CONFLICT (id) DO NOTHING;

-- Insert sample data for meal_recommendations
INSERT INTO meal_recommendations (id, created_at, is_deleted, user_id, request_text, input_ingredient_text, recommendation_type, status) VALUES
('c1111111-1111-1111-1111-111111111111', NOW(), false, '22222222-2222-2222-2222-222222222222', 'Need chicken dinner', 'chicken', 'Dinner', 'Completed'),
('c2222222-2222-2222-2222-222222222222', NOW(), false, '22222222-2222-2222-2222-222222222222', 'Tomato recipes', 'tomato', 'Lunch', 'Completed'),
('c3333333-3333-3333-3333-333333333333', NOW(), false, '33333333-3333-3333-3333-333333333333', 'Rice meals', 'rice', 'Dinner', 'Pending')
ON CONFLICT (id) DO NOTHING;

-- Insert sample data for meal_recommendation_items
INSERT INTO meal_recommendation_items (id, created_at, is_deleted, meal_recommendation_id, recipe_id, match_score, missing_ingredient_count) VALUES
(gen_random_uuid(), NOW(), false, 'c1111111-1111-1111-1111-111111111111', 'b2222222-2222-2222-2222-222222222222', 0.9, 0),
(gen_random_uuid(), NOW(), false, 'c1111111-1111-1111-1111-111111111111', 'b1111111-1111-1111-1111-111111111111', 0.8, 1),
(gen_random_uuid(), NOW(), false, 'c2222222-2222-2222-2222-222222222222', 'b3333333-3333-3333-3333-333333333333', 0.95, 0)
ON CONFLICT (id) DO NOTHING;

-- Insert sample data for recommendation_feedbacks
INSERT INTO recommendation_feedbacks (id, created_at, is_deleted, user_id, meal_recommendation_id, recipe_id, rating, feedback_type, comment) VALUES
(gen_random_uuid(), NOW(), false, '22222222-2222-2222-2222-222222222222', 'c1111111-1111-1111-1111-111111111111', 'b2222222-2222-2222-2222-222222222222', 5, 'Like', 'Great recipe!'),
(gen_random_uuid(), NOW(), false, '22222222-2222-2222-2222-222222222222', 'c1111111-1111-1111-1111-111111111111', 'b1111111-1111-1111-1111-111111111111', 3, 'Neutral', 'A bit too acidic.'),
(gen_random_uuid(), NOW(), false, '22222222-2222-2222-2222-222222222222', 'c2222222-2222-2222-2222-222222222222', 'b3333333-3333-3333-3333-333333333333', 4, 'Like', 'Good for lunch.')
ON CONFLICT (id) DO NOTHING;

-- Insert sample data for media_assets
INSERT INTO media_assets (id, created_at, is_deleted, recipe_id, public_id, url, secure_url, resource_type, format) VALUES
(gen_random_uuid(), NOW(), false, 'b1111111-1111-1111-1111-111111111111', 'img_1', 'http://example.com/img1.jpg', 'https://example.com/img1.jpg', 'image', 'jpg'),
(gen_random_uuid(), NOW(), false, 'b2222222-2222-2222-2222-222222222222', 'img_2', 'http://example.com/img2.jpg', 'https://example.com/img2.jpg', 'image', 'jpg'),
(gen_random_uuid(), NOW(), false, 'b3333333-3333-3333-3333-333333333333', 'img_3', 'http://example.com/img3.jpg', 'https://example.com/img3.jpg', 'image', 'jpg')
ON CONFLICT (id) DO NOTHING;

-- Insert sample data for today_menu_items
INSERT INTO today_menu_items (id, created_at, is_deleted, user_id, recipe_id, meal_name, planned_date, status) VALUES
('d1111111-1111-1111-1111-111111111111', NOW(), false, '22222222-2222-2222-2222-222222222222', 'b1111111-1111-1111-1111-111111111111', 'Tomato Chicken', CURRENT_DATE, 'Cooked'),
('d2222222-2222-2222-2222-222222222222', NOW(), false, '22222222-2222-2222-2222-222222222222', 'b2222222-2222-2222-2222-222222222222', 'Chicken Rice', CURRENT_DATE, 'Planned'),
('d3333333-3333-3333-3333-333333333333', NOW(), false, '33333333-3333-3333-3333-333333333333', 'b3333333-3333-3333-3333-333333333333', 'Tomato Rice', CURRENT_DATE, 'Planned')
ON CONFLICT (id) DO NOTHING;

-- Insert sample data for cooking_logs
INSERT INTO cooking_logs (id, created_at, is_deleted, user_id, today_menu_item_id, recipe_id, meal_name, cooked_at, rating) VALUES
('e1111111-1111-1111-1111-111111111111', NOW(), false, '22222222-2222-2222-2222-222222222222', 'd1111111-1111-1111-1111-111111111111', 'b1111111-1111-1111-1111-111111111111', 'Tomato Chicken', NOW(), 5),
('e2222222-2222-2222-2222-222222222222', NOW(), false, '22222222-2222-2222-2222-222222222222', 'd2222222-2222-2222-2222-222222222222', 'b2222222-2222-2222-2222-222222222222', 'Chicken Rice', NOW(), 4),
('e3333333-3333-3333-3333-333333333333', NOW(), false, '33333333-3333-3333-3333-333333333333', 'd3333333-3333-3333-3333-333333333333', 'b3333333-3333-3333-3333-333333333333', 'Tomato Rice', NOW(), 3)
ON CONFLICT (id) DO NOTHING;

-- Insert sample data for pantry_usage_logs
INSERT INTO pantry_usage_logs (id, created_at, is_deleted, user_id, today_menu_item_id, cooking_log_id, ingredient_id, ingredient_name, quantity_used, unit, action_type) VALUES
(gen_random_uuid(), NOW(), false, '22222222-2222-2222-2222-222222222222', 'd1111111-1111-1111-1111-111111111111', 'e1111111-1111-1111-1111-111111111111', 'a1111111-1111-1111-1111-111111111111', 'Tomato', 0.5, 'kg', 'consumed'),
(gen_random_uuid(), NOW(), false, '22222222-2222-2222-2222-222222222222', 'd1111111-1111-1111-1111-111111111111', 'e1111111-1111-1111-1111-111111111111', 'a2222222-2222-2222-2222-222222222222', 'Chicken Breast', 0.5, 'kg', 'consumed'),
(gen_random_uuid(), NOW(), false, '22222222-2222-2222-2222-222222222222', 'd2222222-2222-2222-2222-222222222222', 'e2222222-2222-2222-2222-222222222222', 'a3333333-3333-3333-3333-333333333333', 'Rice', 0.5, 'kg', 'consumed')
ON CONFLICT (id) DO NOTHING;
