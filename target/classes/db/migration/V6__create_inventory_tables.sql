CREATE TABLE inventory_categories (
    id BIGSERIAL PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inv_categories_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id) ON DELETE CASCADE,
    CONSTRAINT uq_inv_categories_merchant_name UNIQUE (merchant_id, name)
);

CREATE TABLE inventory_items (
    id BIGSERIAL PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    unit VARCHAR(20) NOT NULL,
    current_stock NUMERIC(12, 3) NOT NULL DEFAULT 0.000,
    min_stock NUMERIC(12, 3) NOT NULL DEFAULT 0.000,
    max_stock NUMERIC(12, 3) NOT NULL DEFAULT 0.000,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inv_items_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id) ON DELETE CASCADE,
    CONSTRAINT fk_inv_items_category FOREIGN KEY (category_id) REFERENCES inventory_categories (id) ON DELETE RESTRICT,
    CONSTRAINT uq_inv_items_merchant_name UNIQUE (merchant_id, name)
);

CREATE TABLE inventory_transactions (
    id BIGSERIAL PRIMARY KEY,
    inventory_item_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL, -- STOCK_IN, STOCK_OUT, ADJUSTMENT
    quantity NUMERIC(12, 3) NOT NULL,
    remarks TEXT,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inv_transactions_item FOREIGN KEY (inventory_item_id) REFERENCES inventory_items (id) ON DELETE CASCADE,
    CONSTRAINT fk_inv_transactions_user FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE RESTRICT
);

CREATE INDEX idx_inv_categories_merchant ON inventory_categories (merchant_id);
CREATE INDEX idx_inv_items_merchant_category ON inventory_items (merchant_id, category_id);
CREATE INDEX idx_inv_transactions_item_id ON inventory_transactions (inventory_item_id);
