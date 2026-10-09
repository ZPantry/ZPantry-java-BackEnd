CREATE TABLE shopping_list_items (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL,
    created_by uuid,
    updated_at timestamptz,
    updated_by uuid,
    deleted_at timestamptz,
    deleted_by uuid,
    is_deleted boolean NOT NULL DEFAULT false,
    user_id uuid NOT NULL,
    today_menu_item_id uuid NOT NULL,
    ingredient_id uuid NOT NULL,
    ingredient_name varchar(200) NOT NULL,
    quantity numeric(18,4) NOT NULL,
    unit varchar(50) NOT NULL DEFAULT '',
    status varchar(32) NOT NULL DEFAULT 'PENDING',
    UNIQUE (user_id, today_menu_item_id, ingredient_id, unit)
);

CREATE INDEX shopping_list_items_user_status_idx ON shopping_list_items (user_id, status, created_at DESC);
