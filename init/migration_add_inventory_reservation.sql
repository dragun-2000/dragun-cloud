CREATE TABLE IF NOT EXISTS inventory_reservation
(
    id           bigserial PRIMARY KEY,
    created      timestamp,
    creator      varchar(100),
    updated      timestamp,
    updater      varchar(100),
    order_id     bigint      NOT NULL REFERENCES orders (id),
    variation_id bigint      NOT NULL REFERENCES variations (id),
    quantity     integer     NOT NULL CHECK (quantity > 0),
    status       varchar(16) NOT NULL,
    expires_at   timestamp   NOT NULL,
    stock_deducted boolean   NOT NULL DEFAULT false,
    CONSTRAINT uk_inventory_reservation_order_variation UNIQUE (order_id, variation_id)
);

CREATE INDEX IF NOT EXISTS idx_inventory_reservation_active
    ON inventory_reservation (variation_id, status, expires_at);

CREATE INDEX IF NOT EXISTS idx_inventory_reservation_order
    ON inventory_reservation (order_id);

alter table inventory_reservation owner to dragun;
