CREATE TABLE IF NOT EXISTS unmatched_ingredient_log (
    normalized_name varchar(200) PRIMARY KEY,
    raw_name varchar(500) NOT NULL,
    attempted_names varchar(1000),
    candidate_summary varchar(2000),
    occurrence_count integer NOT NULL DEFAULT 1,
    status varchar(20) NOT NULL DEFAULT 'NEW',
    first_seen_at timestamptz NOT NULL,
    last_seen_at timestamptz NOT NULL
);

CREATE INDEX IF NOT EXISTS unmatched_ingredient_log_review_idx
    ON unmatched_ingredient_log (status, occurrence_count DESC, last_seen_at DESC);
