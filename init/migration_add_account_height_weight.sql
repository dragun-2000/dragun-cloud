-- Migration: Add height and weight columns to account table
-- Date: 2024

ALTER TABLE account 
ADD COLUMN IF NOT EXISTS height INTEGER,
ADD COLUMN IF NOT EXISTS weight INTEGER;

COMMENT ON COLUMN account.height IS 'Chiều cao của người dùng (cm)';
COMMENT ON COLUMN account.weight IS 'Cân nặng của người dùng (kg)';
