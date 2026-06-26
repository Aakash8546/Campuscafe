-- Insert Phase 7 permissions
INSERT INTO permissions (name) VALUES
('DISCOUNT_CREATE'),
('DISCOUNT_VIEW'),
('DISCOUNT_UPDATE'),
('DISCOUNT_DELETE'),
('NOTIFICATION_VIEW'),
('SETTINGS_VIEW'),
('SETTINGS_UPDATE')
ON CONFLICT (name) DO NOTHING;

-- Associate ADMIN with all permissions (including new ones)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Associate MANAGER with Phase 7 permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'MANAGER'
AND p.name IN (
    'DISCOUNT_CREATE',
    'DISCOUNT_VIEW',
    'DISCOUNT_UPDATE',
    'NOTIFICATION_VIEW',
    'SETTINGS_VIEW',
    'SETTINGS_UPDATE'
)
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Associate CASHIER with Phase 7 permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'CASHIER'
AND p.name IN (
    'DISCOUNT_VIEW',
    'NOTIFICATION_VIEW',
    'SETTINGS_VIEW'
)
ON CONFLICT (role_id, permission_id) DO NOTHING;
