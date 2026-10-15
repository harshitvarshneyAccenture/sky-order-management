CREATE TABLE orders
(
    id               UUID PRIMARY KEY,
    customer_id      VARCHAR(64)              NOT NULL,
    status           VARCHAR(20)              NOT NULL,
    total_amount     NUMERIC(19, 2)           NOT NULL CHECK (total_amount >= 0),
    rejection_reason VARCHAR(255),
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    version          BIGINT                   NOT NULL
);

CREATE INDEX idx_orders_customer_created ON orders (customer_id, created_at DESC);

CREATE TABLE order_items
(
    id         BIGSERIAL PRIMARY KEY,
    order_id   UUID           NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    sku        VARCHAR(32)    NOT NULL,
    quantity   INT            NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(19, 2) NOT NULL CHECK (unit_price > 0),
    CONSTRAINT uq_order_items_order_sku UNIQUE (order_id, sku)
);
