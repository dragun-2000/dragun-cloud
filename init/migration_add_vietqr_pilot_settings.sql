-- VietQR pilot: public flag + hashed pilot password (singleton row)

CREATE TABLE IF NOT EXISTS vietqr_pilot_settings
(
    id                    smallint PRIMARY KEY DEFAULT 1,
    visible_to_all        boolean      NOT NULL DEFAULT true,
    pilot_password_hash   varchar(255),
    updated               timestamp,
    updater               varchar(100),
    CONSTRAINT vietqr_pilot_settings_singleton CHECK (id = 1)
);

INSERT INTO vietqr_pilot_settings (id, visible_to_all)
VALUES (1, true)
ON CONFLICT (id) DO NOTHING;

ALTER TABLE vietqr_pilot_settings OWNER TO dragun;
