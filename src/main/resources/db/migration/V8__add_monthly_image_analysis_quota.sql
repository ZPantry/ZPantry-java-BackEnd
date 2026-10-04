CREATE TABLE image_analysis_monthly_usage (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL,
    updated_at timestamptz,
    user_id uuid NOT NULL,
    month_start date NOT NULL,
    used_count integer NOT NULL DEFAULT 0,
    CONSTRAINT image_analysis_monthly_usage_non_negative CHECK (used_count >= 0),
    CONSTRAINT image_analysis_monthly_usage_user_month_unique UNIQUE (user_id, month_start)
);
