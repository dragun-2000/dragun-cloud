-- Migration: Pancake sync fields for order-history (avoid API calls on page load)
-- Adds pancake_order_id, tracking_link, pancake_synced_at to orders

ALTER TABLE orders
ADD COLUMN IF NOT EXISTS pancake_order_id VARCHAR(64),
ADD COLUMN IF NOT EXISTS tracking_link VARCHAR(512),
ADD COLUMN IF NOT EXISTS pancake_synced_at TIMESTAMP;
