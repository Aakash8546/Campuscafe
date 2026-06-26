-- Add priority column to products table with default value of 0
ALTER TABLE products ADD COLUMN priority INTEGER NOT NULL DEFAULT 0;
