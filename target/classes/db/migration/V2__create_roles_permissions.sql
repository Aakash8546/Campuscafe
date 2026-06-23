CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE permissions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE role_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE CASCADE
);

-- Seed roles
INSERT INTO roles (name) VALUES ('ADMIN'), ('MANAGER'), ('CASHIER');

-- Seed permissions
INSERT INTO permissions (name) VALUES
('USER_CREATE'),
('USER_VIEW'),
('USER_UPDATE'),
('PRODUCT_CREATE'),
('PRODUCT_UPDATE'),
('PRODUCT_DELETE'),
('INVENTORY_CREATE'),
('INVENTORY_UPDATE'),
('ORDER_CREATE'),
('ORDER_UPDATE');

-- Associate ADMIN with all permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p 
WHERE r.name = 'ADMIN';

-- Associate MANAGER with most permissions (excludes USER_CREATE, USER_UPDATE, PRODUCT_DELETE)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p 
WHERE r.name = 'MANAGER' 
AND p.name IN ('USER_VIEW', 'PRODUCT_CREATE', 'PRODUCT_UPDATE', 'INVENTORY_CREATE', 'INVENTORY_UPDATE', 'ORDER_CREATE', 'ORDER_UPDATE');

-- Associate CASHIER with basic permissions (USER_VIEW, ORDER_CREATE, ORDER_UPDATE)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p 
WHERE r.name = 'CASHIER' 
AND p.name IN ('USER_VIEW', 'ORDER_CREATE', 'ORDER_UPDATE');
