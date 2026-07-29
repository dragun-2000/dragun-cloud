-- Trừ kho local lúc confirm đơn; tránh oversell sau khi reservation chuyển CONSUMED.
ALTER TABLE inventory_reservation
    ADD COLUMN IF NOT EXISTS stock_deducted boolean NOT NULL DEFAULT false;

COMMENT ON COLUMN inventory_reservation.stock_deducted IS
    'true = đã trừ variations.remain_quantity; không tính lại trong sumActive';
