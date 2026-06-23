-- Insert category permissions
INSERT INTO permissions (name) VALUES
('CATEGORY_CREATE'),
('CATEGORY_VIEW'),
('CATEGORY_UPDATE'),
('CATEGORY_DELETE')
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
    'CATEGORY_CREATE',
    'CATEGORY_VIEW',
    'CATEGORY_UPDATE'
)
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Map CASHIER permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'CASHIER'
AND p.name = 'CATEGORY_VIEW'
ON CONFLICT (role_id, permission_id) DO NOTHING;
