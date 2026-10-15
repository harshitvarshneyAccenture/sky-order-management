CREATE TABLE inventory_items
(
    sku                VARCHAR(32) PRIMARY KEY,
    name               VARCHAR(128)             NOT NULL,
    available_quantity INT                      NOT NULL CHECK (available_quantity >= 0),
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    version            BIGINT                   NOT NULL DEFAULT 0
);

-- One row per processed order: idempotency key + audit of the outcome.
CREATE TABLE reservations
(
    order_id   UUID PRIMARY KEY,
    status     VARCHAR(20)              NOT NULL,
    reason     VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
