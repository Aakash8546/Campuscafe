-- Insert missing permissions
INSERT INTO permissions (name) VALUES
('USER_DELETE'),
('PRODUCT_VIEW'),
('INVENTORY_VIEW'),
('ORDER_VIEW'),
('REPORT_VIEW'),
('SETTINGS_UPDATE')
ON CONFLICT (name) DO NOTHING;

-- Map ADMIN to all permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Map MANAGER permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'MANAGER'
AND p.name IN (
    'USER_VIEW',
    'PRODUCT_CREATE',
    'PRODUCT_VIEW',
    'PRODUCT_UPDATE',
    'INVENTORY_CREATE',
    'INVENTORY_VIEW',
    'INVENTORY_UPDATE',
    'ORDER_VIEW',
    'ORDER_UPDATE',
    'REPORT_VIEW'
)
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Map CASHIER permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'CASHIER'
AND p.name IN (
    'ORDER_CREATE',
    'ORDER_VIEW',
    'ORDER_UPDATE',
    'PRODUCT_VIEW'
)
ON CONFLICT (role_id, permission_id) DO NOTHING;
