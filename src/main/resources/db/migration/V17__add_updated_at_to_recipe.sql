-- Add updated_at column to product_recipes table
ALTER TABLE product_recipes ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;
