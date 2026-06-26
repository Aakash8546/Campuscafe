-- Insert Phase 8 permissions
INSERT INTO permissions (name) VALUES
('DASHBOARD_VIEW')
ON CONFLICT (name) DO NOTHING;

-- Associate ADMIN with all permissions (including new ones)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Associate MANAGER with DASHBOARD_VIEW
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'MANAGER'
AND p.name = 'DASHBOARD_VIEW'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Associate CASHIER with DASHBOARD_VIEW
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'CASHIER'
AND p.name = 'DASHBOARD_VIEW'
ON CONFLICT (role_id, permission_id) DO NOTHING;
