-- Migration: Add count_error and message_error columns to orders table
-- Date: 2024
-- Description: Add fields to track Pancake POS sync errors

ALTER TABLE orders 
ADD COLUMN IF NOT EXISTS count_error INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS message_error VARCHAR(1000);

-- Update existing orders to have default values
UPDATE orders 
SET count_error = 0 
WHERE count_error IS NULL;

