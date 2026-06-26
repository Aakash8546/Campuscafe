-- Create order sequence starting at 1001
CREATE SEQUENCE order_number_sequence START WITH 1001 INCREMENT BY 1;

-- Add priority column to orders
ALTER TABLE orders ADD COLUMN priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM';
