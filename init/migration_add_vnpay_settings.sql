-- VNPAY admin on/off (singleton). env/properties vẫn phải configured; cờ này chỉ ẩn/hiện tính năng trên storefront.

CREATE TABLE IF NOT EXISTS vnpay_settings
(
    id                   smallint PRIMARY KEY DEFAULT 1,
    enabled              boolean      NOT NULL DEFAULT true,
    visible_to_all       boolean      NOT NULL DEFAULT false,
    pilot_password_hash  varchar(255),
    updated              timestamp,
    updater              varchar(100),
    CONSTRAINT vnpay_settings_singleton CHECK (id = 1)
);

INSERT INTO vnpay_settings (id, enabled, visible_to_all)
VALUES (1, true, false)
ON CONFLICT (id) DO NOTHING;

ALTER TABLE vnpay_settings OWNER TO dragun;
