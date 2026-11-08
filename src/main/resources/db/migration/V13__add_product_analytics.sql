-- Additive event store. It intentionally starts empty: analytics must never fabricate history.
CREATE TABLE analytics_events (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL,
    user_id uuid,
    session_id varchar(100),
    event_name varchar(64) NOT NULL,
    screen_name varchar(100),
    properties jsonb NOT NULL DEFAULT '{}'::jsonb
);
CREATE INDEX analytics_events_created_idx ON analytics_events (created_at DESC);
CREATE INDEX analytics_events_user_created_idx ON analytics_events (user_id, created_at DESC);
CREATE INDEX analytics_events_name_created_idx ON analytics_events (event_name, created_at DESC);

CREATE TABLE ai_recognition_feedbacks (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL,
    updated_at timestamptz,
    user_id uuid NOT NULL,
    analysis_id uuid NOT NULL,
    feedback varchar(16) NOT NULL,
    reason varchar(300),
    correction varchar(500),
    CONSTRAINT ai_recognition_feedback_value CHECK (feedback IN ('LIKE', 'DISLIKE')),
    CONSTRAINT ai_recognition_feedback_unique UNIQUE (user_id, analysis_id)
);
CREATE INDEX ai_recognition_feedback_created_idx ON ai_recognition_feedbacks (created_at DESC);
