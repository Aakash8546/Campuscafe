-- Alter merchants table to handle multi-state verification status
ALTER TABLE merchants RENAME COLUMN verified TO temp_verified;
ALTER TABLE merchants ADD COLUMN verified VARCHAR(20) NOT NULL DEFAULT 'PENDING';
UPDATE merchants SET verified = 'VERIFIED' WHERE temp_verified = TRUE;
UPDATE merchants SET verified = 'PENDING' WHERE temp_verified = FALSE;
ALTER TABLE merchants DROP COLUMN temp_verified;

-- Add email verification flag and super admin action token
ALTER TABLE merchants ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE merchants SET email_verified = TRUE WHERE verified = 'VERIFIED';
ALTER TABLE merchants ADD COLUMN super_admin_token VARCHAR(100);

-- Seed SUPER_ADMIN role
INSERT INTO roles (name) VALUES ('SUPER_ADMIN') ON CONFLICT (name) DO NOTHING;

-- Seed System Admin Merchant
INSERT INTO merchants (cafe_name, email, phone, address, city, pincode, verified, email_verified, active)
VALUES ('System Admin Merchant', 'aakashsrivastava2151@gmail.com', '+919999999999', 'System Address', 'System City', '110001', 'VERIFIED', TRUE, TRUE)
ON CONFLICT (email) DO NOTHING;

-- Seed Super Admin User (linked to the system merchant, password: 'Password123!')
INSERT INTO users (merchant_id, role_id, name, email, phone, password, active)
SELECT 
    m.id, 
    r.id, 
    'Super Admin', 
    'aakashsrivastava2151@gmail.com', 
    '+919999999999', 
    '$2a$10$wK1Wb6i0c3xG60U0U2pCkeZqF4E07bVwY03gq8iX9xGj3kUjWw/F6', -- bcrypt hash for Password123!
    TRUE
FROM merchants m, roles r
WHERE m.email = 'aakashsrivastava2151@gmail.com' AND r.name = 'SUPER_ADMIN'
ON CONFLICT (email) DO NOTHING;
