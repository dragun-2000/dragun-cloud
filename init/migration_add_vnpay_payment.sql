-- VNPAY: một dòng cho mỗi lần tạo URL thanh toán (vnp_TxnRef = orders.code).
-- Tách riêng khỏi checkout_pending (VietQR).

CREATE TABLE IF NOT EXISTS vnpay_payment
(
    id                     bigserial PRIMARY KEY,
    txn_ref                varchar(100)   NOT NULL,
    order_id               bigint         NOT NULL,
    account_id             bigint         NOT NULL,
    amount                 numeric(19, 2) NOT NULL,
    status                 varchar(16)    NOT NULL,
    order_info             varchar(255)   NOT NULL,
    client_ip              varchar(45),
    vnp_create_date        varchar(14)    NOT NULL,
    expires_at             timestamp      NOT NULL,
    vnp_transaction_no     varchar(15),
    vnp_bank_code          varchar(20),
    vnp_bank_tran_no       varchar(255),
    vnp_card_type          varchar(20),
    vnp_pay_date           varchar(14),
    vnp_response_code      varchar(2),
    vnp_transaction_status varchar(2),
    confirmed_by           varchar(16),
    confirmed_at           timestamp,
    created                timestamp,
    creator                varchar(100),
    updated                timestamp,
    updater                varchar(100),
    CONSTRAINT uk_vnpay_payment_txn_ref UNIQUE (txn_ref)
);

CREATE INDEX IF NOT EXISTS idx_vnpay_payment_status_expires ON vnpay_payment (status, expires_at);
CREATE INDEX IF NOT EXISTS idx_vnpay_payment_order_id ON vnpay_payment (order_id);

ALTER TABLE vnpay_payment OWNER TO dragun;
