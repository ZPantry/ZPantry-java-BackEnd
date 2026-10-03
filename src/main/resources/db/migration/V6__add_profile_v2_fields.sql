ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS birth_date date;
ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS activity_level varchar(32);
ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS goals text;
ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS bmr numeric(10,2);
ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS tdee numeric(10,2);
ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS daily_calorie_target numeric(10,2);
ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS daily_protein_target numeric(10,2);
