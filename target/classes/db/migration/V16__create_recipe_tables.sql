CREATE TABLE product_recipes (
    id BIGSERIAL PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    inventory_item_id BIGINT NOT NULL,
    quantity_required NUMERIC(12, 3) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_recipes_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id) ON DELETE CASCADE,
    CONSTRAINT fk_product_recipes_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT fk_product_recipes_inventory_item FOREIGN KEY (inventory_item_id) REFERENCES inventory_items (id) ON DELETE CASCADE,
    CONSTRAINT uq_product_recipes_product_item UNIQUE (product_id, inventory_item_id)
);

-- Seed new Recipe permissions
INSERT INTO permissions (name) VALUES
('RECIPE_CREATE'),
('RECIPE_VIEW'),
('RECIPE_UPDATE'),
('RECIPE_DELETE')
ON CONFLICT (name) DO NOTHING;

-- Map Recipe permissions to ADMIN and MANAGER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name IN ('ADMIN', 'MANAGER')
AND p.name IN ('RECIPE_CREATE', 'RECIPE_VIEW', 'RECIPE_UPDATE', 'RECIPE_DELETE')
ON CONFLICT (role_id, permission_id) DO NOTHING;
