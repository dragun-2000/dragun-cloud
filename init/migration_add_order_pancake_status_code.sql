-- Migration: store Pancake numeric status for cancel / sync classification
ALTER TABLE orders
ADD COLUMN IF NOT EXISTS pancake_status_code INTEGER;
