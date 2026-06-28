-- Seed KITCHEN_STAFF role
INSERT INTO roles (name) VALUES ('KITCHEN_STAFF') ON CONFLICT (name) DO NOTHING;

-- Seed KOT permissions
INSERT INTO permissions (name) VALUES ('KOT_VIEW'), ('KOT_UPDATE') ON CONFLICT (name) DO NOTHING;

-- Associate ADMIN, MANAGER, KITCHEN_STAFF with KOT permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p 
WHERE r.name IN ('ADMIN', 'MANAGER', 'KITCHEN_STAFF') 
AND p.name IN ('KOT_VIEW', 'KOT_UPDATE', 'ORDER_VIEW', 'ORDER_UPDATE')
ON CONFLICT DO NOTHING;

-- Add instructions to order_items
ALTER TABLE order_items ADD COLUMN instructions VARCHAR(255);

-- Remove priority column from orders
ALTER TABLE orders DROP COLUMN IF EXISTS priority;
