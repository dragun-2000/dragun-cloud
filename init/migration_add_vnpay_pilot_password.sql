-- VNPAY pilot password (giống VietQR): visible_to_all = false → cần mật khẩu trên trang thanh toán.

ALTER TABLE vnpay_settings
    ADD COLUMN IF NOT EXISTS visible_to_all boolean NOT NULL DEFAULT false;

ALTER TABLE vnpay_settings
    ADD COLUMN IF NOT EXISTS pilot_password_hash varchar(255);

-- Prod test: mặc định pilot (chưa public). Admin đặt mật khẩu trên AM011.
UPDATE vnpay_settings
SET visible_to_all = false
WHERE id = 1
  AND visible_to_all IS DISTINCT FROM true;
