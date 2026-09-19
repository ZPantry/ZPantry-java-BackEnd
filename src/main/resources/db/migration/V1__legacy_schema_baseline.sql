CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE users (
    id uuid PRIMARY KEY, created_at timestamptz NOT NULL, created_by uuid,
    updated_at timestamptz, updated_by uuid, deleted_at timestamptz, deleted_by uuid,
    is_deleted boolean NOT NULL DEFAULT false, full_name varchar(150),
    email varchar(200) NOT NULL UNIQUE, avatar_url varchar(500),
    password_hashed varchar(500) NOT NULL, otp_code varchar(6), otp_expired_at timestamptz,
    otp_retry_count integer NOT NULL DEFAULT 0, is_email_confirmed boolean NOT NULL DEFAULT false,
    is_active boolean NOT NULL DEFAULT false, role varchar(50) NOT NULL DEFAULT 'user',
    refresh_token_hash varchar(128), refresh_token_expires_at timestamptz
);

CREATE TABLE ingredients (
    id uuid PRIMARY KEY, created_at timestamptz NOT NULL, created_by uuid,
    updated_at timestamptz, updated_by uuid, deleted_at timestamptz, deleted_by uuid,
    is_deleted boolean NOT NULL DEFAULT false, name varchar(200) NOT NULL,
    normalized_name varchar(200) NOT NULL, category varchar(100), unit varchar(50),
    calories_per_unit numeric(18,4), protein_per_unit numeric(18,4),
    fat_per_unit numeric(18,4), carb_per_unit numeric(18,4), image_url varchar(500),
    gradient_from varchar(32), gradient_to varchar(32), embedding vector(1536)
);

CREATE TABLE ingredient_aliases (
    id uuid PRIMARY KEY, created_at timestamptz NOT NULL, created_by uuid,
    updated_at timestamptz, updated_by uuid, deleted_at timestamptz, deleted_by uuid,
    is_deleted boolean NOT NULL DEFAULT false, ingredient_id uuid NOT NULL,
    alias_name varchar(200) NOT NULL, normalized_alias_name varchar(200) NOT NULL
);

CREATE TABLE recipes (
    id uuid PRIMARY KEY, created_at timestamptz NOT NULL, created_by uuid,
    updated_at timestamptz, updated_by uuid, deleted_at timestamptz, deleted_by uuid,
    is_deleted boolean NOT NULL DEFAULT false, name varchar(200) NOT NULL, description text,
    cooking_time_minutes integer, difficulty varchar(50), serving_size integer,
    instruction_text text, image_url varchar(500), source_type varchar(100),
    gradient_from varchar(32), gradient_to varchar(32), embedding vector(1536)
);

CREATE TABLE recipe_ingredients (
    id uuid PRIMARY KEY, created_at timestamptz NOT NULL, created_by uuid,
    updated_at timestamptz, updated_by uuid, deleted_at timestamptz, deleted_by uuid,
    is_deleted boolean NOT NULL DEFAULT false, recipe_id uuid NOT NULL,
    ingredient_id uuid NOT NULL, quantity numeric(18,4), unit varchar(50),
    is_required boolean NOT NULL DEFAULT true, note text
);

CREATE TABLE user_pantry_items (
    id uuid PRIMARY KEY, created_at timestamptz NOT NULL, created_by uuid,
    updated_at timestamptz, updated_by uuid, deleted_at timestamptz, deleted_by uuid,
    is_deleted boolean NOT NULL DEFAULT false, user_id uuid NOT NULL,
    ingredient_id uuid NOT NULL, quantity numeric(18,4), unit varchar(50),
    expired_at timestamptz, storage_location varchar(100), note text
);

CREATE TABLE meal_recommendations (
    id uuid PRIMARY KEY, created_at timestamptz NOT NULL, created_by uuid,
    updated_at timestamptz, updated_by uuid, deleted_at timestamptz, deleted_by uuid,
    is_deleted boolean NOT NULL DEFAULT false, user_id uuid NOT NULL, request_text text,
    input_ingredient_text text, recommendation_type varchar(100), status varchar(100),
    completed_at timestamptz
);

CREATE TABLE meal_recommendation_items (
    id uuid PRIMARY KEY, created_at timestamptz NOT NULL, created_by uuid,
    updated_at timestamptz, updated_by uuid, deleted_at timestamptz, deleted_by uuid,
    is_deleted boolean NOT NULL DEFAULT false, meal_recommendation_id uuid NOT NULL,
    recipe_id uuid NOT NULL, match_score numeric(18,4),
    missing_ingredient_count integer NOT NULL DEFAULT 0, missing_ingredient_names varchar(2000),
    reason varchar(2000), rank integer NOT NULL DEFAULT 0
);

CREATE TABLE recommendation_feedbacks (
    id uuid PRIMARY KEY, created_at timestamptz NOT NULL, created_by uuid,
    updated_at timestamptz, updated_by uuid, deleted_at timestamptz, deleted_by uuid,
    is_deleted boolean NOT NULL DEFAULT false, user_id uuid NOT NULL,
    meal_recommendation_id uuid NOT NULL, recipe_id uuid NOT NULL, rating integer,
    feedback_type varchar(100), comment varchar(2000)
);

CREATE TABLE media_assets (
    id uuid PRIMARY KEY, created_at timestamptz NOT NULL, created_by uuid,
    updated_at timestamptz, updated_by uuid, deleted_at timestamptz, deleted_by uuid,
    is_deleted boolean NOT NULL DEFAULT false, recipe_id uuid, ingredient_id uuid,
    public_id varchar(200) NOT NULL, url varchar(500) NOT NULL, secure_url varchar(500) NOT NULL,
    resource_type varchar(50), format varchar(50), width integer, height integer
);

CREATE TABLE today_menu_items (
    id uuid PRIMARY KEY, created_at timestamptz NOT NULL, created_by uuid,
    updated_at timestamptz, updated_by uuid, deleted_at timestamptz, deleted_by uuid,
    is_deleted boolean NOT NULL DEFAULT false, user_id uuid NOT NULL, meal_id uuid,
    recipe_id uuid, meal_name varchar(200) NOT NULL, meal_type varchar(100), serving_size integer,
    planned_date date NOT NULL, status varchar(20) NOT NULL DEFAULT 'Planned', note text,
    cooked_at timestamptz, image_url varchar(500), image_public_id varchar(200)
);

CREATE TABLE cooking_logs (
    id uuid PRIMARY KEY, created_at timestamptz NOT NULL, created_by uuid,
    updated_at timestamptz, updated_by uuid, deleted_at timestamptz, deleted_by uuid,
    is_deleted boolean NOT NULL DEFAULT false, user_id uuid NOT NULL,
    today_menu_item_id uuid NOT NULL, meal_id uuid, recipe_id uuid,
    meal_name varchar(200) NOT NULL, image_url varchar(500), image_public_id varchar(200),
    cooked_at timestamptz NOT NULL, rating integer, note text
);

CREATE TABLE pantry_usage_logs (
    id uuid PRIMARY KEY, created_at timestamptz NOT NULL, created_by uuid,
    updated_at timestamptz, updated_by uuid, deleted_at timestamptz, deleted_by uuid,
    is_deleted boolean NOT NULL DEFAULT false, user_id uuid NOT NULL,
    today_menu_item_id uuid NOT NULL, cooking_log_id uuid NOT NULL, ingredient_id uuid NOT NULL,
    ingredient_name varchar(200) NOT NULL, quantity_used numeric(18,4), unit varchar(50),
    action_type varchar(50) NOT NULL DEFAULT 'consumed', warning text
);
