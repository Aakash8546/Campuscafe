CREATE TABLE discounts (
    id BIGSERIAL PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    discount_type VARCHAR(20) NOT NULL, -- PERCENTAGE, FLAT
    value NUMERIC(12, 2) NOT NULL,
    max_discount NUMERIC(12, 2),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_discounts_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id) ON DELETE CASCADE,
    CONSTRAINT uq_discounts_merchant_name UNIQUE (merchant_id, name)
);

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL, -- ORDER, LOW_STOCK, SYSTEM
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    read_status BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifications_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id) ON DELETE CASCADE
);

CREATE INDEX idx_discounts_merchant_active ON discounts (merchant_id, active);
CREATE INDEX idx_notifications_merchant_unread ON notifications (merchant_id, read_status);
CREATE INDEX idx_notifications_merchant_created ON notifications (merchant_id, created_at DESC);
