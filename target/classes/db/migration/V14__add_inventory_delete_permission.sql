-- Seed missing INVENTORY_DELETE permission
INSERT INTO permissions (name) VALUES ('INVENTORY_DELETE') ON CONFLICT (name) DO NOTHING;

-- Map INVENTORY_DELETE to ADMIN role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'ADMIN' AND p.name = 'INVENTORY_DELETE'
ON CONFLICT (role_id, permission_id) DO NOTHING;
