CREATE TABLE subscription_plans (
    id uuid PRIMARY KEY, created_at timestamptz NOT NULL, updated_at timestamptz,
    code varchar(32) NOT NULL UNIQUE, name varchar(100) NOT NULL, price_vnd numeric(18,0) NOT NULL,
    duration_days integer NOT NULL, active boolean NOT NULL DEFAULT true
);
INSERT INTO subscription_plans (id, created_at, code, name, price_vnd, duration_days) VALUES
('00000000-0000-0000-0000-000000000901', now(), 'Z_FREE', 'Z-Free', 0, 0),
('00000000-0000-0000-0000-000000000902', now(), 'Z_PLUS', 'Z-Plus', 49000, 30);

CREATE TABLE user_subscriptions (
    id uuid PRIMARY KEY, created_at timestamptz NOT NULL, updated_at timestamptz,
    user_id uuid NOT NULL, plan_code varchar(32) NOT NULL, status varchar(32) NOT NULL,
    started_at timestamptz NOT NULL, expires_at timestamptz, cancelled_at timestamptz,
    provider varchar(32), auto_renew boolean NOT NULL DEFAULT false
);
CREATE INDEX user_subscriptions_user_active_idx ON user_subscriptions (user_id, status, expires_at);

CREATE TABLE subscription_usage (
    id uuid PRIMARY KEY, created_at timestamptz NOT NULL, updated_at timestamptz,
    user_id uuid NOT NULL, feature varchar(64) NOT NULL, period_start date NOT NULL, used_count integer NOT NULL DEFAULT 0,
    UNIQUE (user_id, feature, period_start)
);

CREATE TABLE payment_transactions (
    id uuid PRIMARY KEY, created_at timestamptz NOT NULL, updated_at timestamptz,
    user_id uuid NOT NULL, subscription_id uuid, provider varchar(32) NOT NULL,
    merchant_order_id varchar(100) NOT NULL UNIQUE, provider_transaction_id varchar(200),
    amount_vnd numeric(18,0) NOT NULL, status varchar(32) NOT NULL, checkout_url text,
    paid_at timestamptz, failure_reason varchar(300)
);
CREATE INDEX payment_transactions_user_created_idx ON payment_transactions (user_id, created_at DESC);
