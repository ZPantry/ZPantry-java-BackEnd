CREATE TABLE user_profiles (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL,
    created_by uuid,
    updated_at timestamptz,
    updated_by uuid,
    deleted_at timestamptz,
    deleted_by uuid,
    is_deleted boolean NOT NULL DEFAULT false,
    user_id uuid NOT NULL UNIQUE,
    age integer,
    gender varchar(50),
    height numeric(5,2),
    weight numeric(5,2),
    goal varchar(200),
    diet_preference varchar(500),
    allergies text
);

-- Insert sample profiles using user_ids from V2__insert_sample_data.sql
INSERT INTO user_profiles (id, created_at, is_deleted, user_id, age, gender, height, weight, goal, diet_preference, allergies) VALUES
('f1111111-1111-1111-1111-111111111111', NOW(), false, '22222222-2222-2222-2222-222222222222', 25, 'Female', 165.5, 55.0, 'Weight loss', 'Keto', 'Peanuts'),
('f2222222-2222-2222-2222-222222222222', NOW(), false, '33333333-3333-3333-3333-333333333333', 30, 'Male', 180.0, 75.5, 'Muscle gain', 'High protein', 'None')
ON CONFLICT (id) DO NOTHING;
