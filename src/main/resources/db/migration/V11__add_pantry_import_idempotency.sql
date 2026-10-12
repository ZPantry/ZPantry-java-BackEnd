CREATE TABLE IF NOT EXISTS pantry_import_analyses (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL,
    user_id uuid NOT NULL,
    image_type varchar(30) NOT NULL,
    status varchar(30) NOT NULL,
    expires_at timestamptz NOT NULL
);

CREATE TABLE IF NOT EXISTS pantry_import_idempotency (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL,
    user_id uuid NOT NULL,
    idempotency_key varchar(100) NOT NULL,
    analysis_id uuid,
    response_json text,
    UNIQUE (user_id, idempotency_key)
);

CREATE INDEX IF NOT EXISTS pantry_import_analyses_user_expires_idx
    ON pantry_import_analyses (user_id, expires_at);
