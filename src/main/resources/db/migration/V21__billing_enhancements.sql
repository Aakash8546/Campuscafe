ALTER TABLE merchants ADD COLUMN next_bill_serial BIGINT NOT NULL DEFAULT 0;
ALTER TABLE orders ADD COLUMN bill_serial_number BIGINT;
