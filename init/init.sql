create sequence account_seq
    increment by 50;

alter sequence account_seq owner to dragun;

create sequence mail_history_seq
    increment by 50;

alter sequence mail_history_seq owner to dragun;

create sequence thiyen_products_seq
    increment by 50;

alter sequence thiyen_products_seq owner to dragun;

CREATE TABLE IF NOT EXISTS account
(
    id             bigint                not null
        primary key,
    created        timestamp,
    creator        varchar(100),
    updated        timestamp,
    updater        varchar(100),
    account_status varchar(255),
    address        varchar(200),
    authorities    varchar(20)           not null,
    deleted        boolean               not null,
    district       varchar(200),
    full_name      varchar(100),
    hash           varchar(100),
    logout         boolean,
    mail_address   varchar(200),
    password       varchar(255)          not null,
    phone          varchar(11),
    province       varchar(200),
    ward           varchar(200),
    first_login    boolean default false not null,
    floor          varchar(200)
);

alter table account
    owner to dragun;

CREATE TABLE IF NOT EXISTS carts
(
    cart_id    bigserial
        primary key,
    created    timestamp,
    creator    varchar(100),
    updated    timestamp,
    updater    varchar(100),
    account_id bigint not null
        constraint fktbh18csnlmy9mre0klfe4m941
            references account
);

alter table carts
    owner to dragun;

CREATE TABLE IF NOT EXISTS category
(
    id          bigserial
        primary key,
    created     timestamp,
    creator     varchar(100),
    updated     timestamp,
    updater     varchar(100),
    deleted     boolean not null,
    description varchar(255),
    name        varchar(255)
);

alter table category
    owner to dragun;

CREATE TABLE IF NOT EXISTS districts
(
    id             bigserial
        primary key,
    code           varchar(255),
    name           varchar(255),
    name_with_type varchar(255),
    parent_code    varchar(255),
    path           varchar(255),
    path_with_type varchar(255),
    slug           varchar(255),
    type           varchar(255)
);

alter table districts
    owner to dragun;

CREATE TABLE IF NOT EXISTS mail_history
(
    id                 bigint not null
        primary key,
    approve            boolean default false,
    content            varchar(255),
    email_address      varchar(255),
    master_convert_id  bigint,
    send_time          timestamp,
    user_customer_code varchar(255)
);

alter table mail_history
    owner to dragun;

CREATE TABLE IF NOT EXISTS order_sequence
(
    id        bigserial
        primary key,
    date      varchar(255),
    max_order integer not null
);

alter table order_sequence
    owner to dragun;

CREATE TABLE IF NOT EXISTS orders
(
    id               bigserial
        primary key,
    created          timestamp,
    creator          varchar(100),
    updated          timestamp,
    updater          varchar(100),
    code             varchar(255),
    payment_method   varchar(255),
    shipping_address varchar(255),
    status               varchar(255),
    pancake_status_name  varchar(255),
    shipping_partner     varchar(64),
    pancake_order_id     varchar(64),
    tracking_link        varchar(512),
    pancake_synced_at    timestamp,
    total_amount         double precision not null,
    account_id       bigint           not null
        constraint fk3c7gbsfawn58r27cf5b2km72f
            references account,
    email            varchar(255),
    full_name        varchar(255),
    note             varchar(255),
    phone            varchar(255),
    voucher          varchar(255),
    prepaid          numeric(19, 2),
    shipping_fee     numeric(19),
    deleted          boolean default false
);

alter table orders
    owner to dragun;

CREATE TABLE IF NOT EXISTS payments
(
    id             bigserial
        primary key,
    created        timestamp,
    creator        varchar(100),
    updated        timestamp,
    updater        varchar(100),
    amount         double precision not null,
    payment_method varchar(255),
    payment_status varchar(255),
    transaction_id varchar(255),
    order_id       bigint           not null
        constraint fk81gagumt0r8y3rmudcgpbk42l
            references orders
);

alter table payments
    owner to dragun;

CREATE TABLE IF NOT EXISTS products
(
    id                 bigserial
        primary key,
    created            timestamp,
    creator            varchar(100),
    updated            timestamp,
    updater            varchar(100),
    code               varchar(255),
    colors             varchar(255),
    deleted            boolean          not null,
    description        text,
    image              varchar(255),
    image1             varchar(255),
    image2             varchar(255),
    image3             varchar(255),
    name               varchar(255),
    price              double precision not null,
    related_product1   varchar(255),
    related_product2   varchar(255),
    related_product3   varchar(255),
    related_product4   varchar(255),
    sizes              varchar(255),
    stock_quantity     integer          not null,
    sub_code           varchar(255),
    category_id        bigint
        constraint fk1cf90etcu98x1e6n9aks3tel3
            references category,
    product_pancake_id varchar(255),
    variation_id       varchar(255),
    display_id         varchar(100),
    discount           numeric(19) default 0,
    discount_price     numeric(19),
    image4             varchar(255),
    image5             varchar(255),
    image6             varchar(255),
    image7             varchar(255),
    image8             varchar(255),
    image9             varchar(255),
    description_size   text,
    category           varchar(255),
    categories         varchar(255),
    final_price        numeric(19)
);

alter table products
    owner to dragun;

CREATE TABLE IF NOT EXISTS province
(
    id             bigserial
        primary key,
    code           varchar(255),
    name           varchar(255),
    name_with_type varchar(255),
    slug           varchar(255),
    type           varchar(255),
    deleted        boolean default false
);

alter table province
    owner to dragun;

CREATE TABLE IF NOT EXISTS wards
(
    id             bigserial
        primary key,
    code           varchar(255),
    name           varchar(255),
    name_with_type varchar(255),
    parent_code    varchar(255),
    path           varchar(255),
    path_with_type varchar(255),
    slug           varchar(255),
    type           varchar(255)
);

alter table wards
    owner to dragun;

CREATE TABLE IF NOT EXISTS vouchers
(
    id               bigserial
        primary key,
    code             varchar(255),
    discount_percent integer,
    discount_price   integer,
    name             varchar(255),
    deleted          boolean,
    shipping_fee     integer,
    end_date         date,
    start_date       date
);

alter table vouchers
    owner to dragun;

CREATE TABLE IF NOT EXISTS pancake_properties
(
    id           bigserial
        primary key,
    created      timestamp,
    creator      varchar(100),
    updated      timestamp,
    updater      varchar(100),
    shop_id      varchar(255),
    token        varchar(255),
    warehouse_id varchar(255),
    deleted      boolean default false not null,
    page_id      varchar(255),
    seller_id    varchar(255)
);

alter table pancake_properties
    owner to dragun;

CREATE TABLE IF NOT EXISTS webhook_history
(
    id      bigserial
        primary key,
    created timestamp,
    creator varchar(100),
    updated timestamp,
    updater varchar(100),
    payload text
);

alter table webhook_history
    owner to dragun;

CREATE TABLE IF NOT EXISTS variations
(
    id                     bigserial
        primary key,
    created                timestamp,
    creator                varchar(100),
    updated                timestamp,
    updater                varchar(100),
    deleted                boolean not null,
    name                   varchar(255),
    product_id             bigint,
    variation_id           varchar(255),
    color                  varchar(255),
    display_id             varchar(255),
    pancake_product_id     varchar(255),
    remain_quantity        bigint,
    retail_price           bigint,
    size                   varchar(255),
    actual_remain_quantity bigint,
    returning_quantity     bigint,
    total_quantity         bigint,
    waiting_quantity       bigint,
    image                  varchar(255)
);

alter table variations
    owner to dragun;

CREATE TABLE IF NOT EXISTS cart_items
(
    id           bigserial
        primary key,
    created      timestamp,
    creator      varchar(100),
    updated      timestamp,
    updater      varchar(100),
    quantity     integer          not null,
    total_price  double precision not null,
    cart_id      bigint           not null
        constraint fkpcttvuq4mxppo8sxggjtn5i2c
            references carts,
    option       varchar(255),
    variation_id bigint           not null
        constraint fkiai9ufmlxuo3rn7kansfwa8sv
            references variations,
    image        varchar(255)
);

alter table cart_items
    owner to dragun;

CREATE TABLE IF NOT EXISTS order_items
(
    id             bigserial
        primary key,
    created        timestamp,
    creator        varchar(100),
    updated        timestamp,
    updater        varchar(100),
    price          double precision not null,
    quantity       integer          not null,
    order_id       bigint           not null
        constraint fkbioxgbv59vetrxe0ejfubep1w
            references orders,
    option         varchar(255),
    variation_id   bigint           not null
        constraint fk6yqwtkipqm7skt1v1gqtix5b9
            references variations,
    discount_price numeric(19, 2),
    final_price    numeric(19, 2)
);

alter table order_items
    owner to dragun;

CREATE TABLE IF NOT EXISTS warehouse
(
    id           bigserial
        primary key,
    deleted      boolean,
    district_id  varchar(255),
    full_address varchar(255),
    name         varchar(255),
    phone_number varchar(255),
    province_id  varchar(255)
);

alter table warehouse
    owner to dragun;

CREATE TABLE IF NOT EXISTS category_relations
(
    id          bigserial
        primary key,
    created     timestamp,
    creator     varchar(100),
    updated     timestamp,
    updater     varchar(100),
    category_id bigint,
    product_id  bigint
);

alter table category_relations
    owner to dragun;

CREATE TABLE IF NOT EXISTS users
(
    id              serial
        primary key,
    email           varchar,
    username        varchar,
    hashed_password varchar,
    is_active       boolean,
    is_admin        boolean
);

alter table users
    owner to dragun;

create index ix_users_id
    on users (id);

create unique index ix_users_email
    on users (email);

create unique index ix_users_username
    on users (username);

CREATE TABLE IF NOT EXISTS categories
(
    id          serial
        primary key,
    name        varchar,
    description varchar
);

alter table categories
    owner to dragun;

create unique index ix_categories_name
    on categories (name);

create index ix_categories_id
    on categories (id);

CREATE TABLE IF NOT EXISTS products_extend
(
    id          bigserial
        primary key,
    created_at  timestamp,
    description varchar(500),
    image_url   varchar(500),
    name        varchar(255)                                         not null,
    price       double precision                                     not null,
    shop_id     bigint,
    updated_at  timestamp,
    category    varchar(20) default 'BEST_SELLER'::character varying not null
);

alter table products_extend
    owner to dragun;

CREATE TABLE IF NOT EXISTS shops_extend
(
    id         bigserial
        primary key,
    address    varchar(255)                                  not null,
    created_at timestamp,
    name       varchar(255)                                  not null,
    phone      varchar(255)                                  not null,
    updated_at timestamp,
    category   varchar(20) default 'FOOD'::character varying not null,
    image_url  varchar(200),
    code       varchar(50)
);

alter table shops_extend
    owner to dragun;

CREATE TABLE IF NOT EXISTS orders_extend
(
    id         bigserial
        primary key,
    created_at timestamp,
    address    varchar(255),
    name       varchar(255),
    phone      varchar(255),
    status     varchar(255),
    total      double precision not null,
    type       varchar(255),
    updated_at timestamp
);

alter table orders_extend
    owner to dragun;

CREATE TABLE IF NOT EXISTS order_extend_items
(
    order_extend_id bigint           not null
        constraint fkjkkcbnetxdcxuf4yi6vs086jl
            references orders_extend,
    price           double precision not null,
    product_id      varchar(255),
    quantity        integer          not null,
    subtotal        double precision not null,
    image_url       varchar(255)
);

alter table order_extend_items
    owner to dragun;

CREATE TABLE IF NOT EXISTS thiyen_products
(
    id             bigint not null
        primary key,
    benefits       text,
    bulk_price     double precision,
    bulk_quantity  varchar(255),
    detailed_usage text,
    discount       integer,
    image          varchar(255),
    ingredients    text,
    is_new         boolean,
    manufacturer   varchar(255),
    name           varchar(255),
    old_price      double precision,
    price          double precision,
    quantity       varchar(255),
    review_count   integer,
    short_desc     text,
    specifications text,
    storage        text,
    target_users   text,
    technology     text,
    usage          text,
    deleted        boolean default false,
    category       varchar(255)
);

alter table thiyen_products
    owner to dragun;

CREATE TABLE IF NOT EXISTS product_gallery
(
    id         bigserial
        primary key,
    image_url  varchar(255),
    position   integer,
    product_id integer
        constraint fke4d1gxvp1e711dhq4xjtwjpim
            references thiyen_products
);

alter table product_gallery
    owner to dragun;

CREATE TABLE IF NOT EXISTS product_variants
(
    id         bigserial
        primary key,
    label      varchar(255),
    value      varchar(255),
    product_id integer
        constraint fkhq8mi2mfg4qmnjs97f19wq41e
            references thiyen_products
);

alter table product_variants
    owner to dragun;

CREATE TABLE IF NOT EXISTS checkout_pending
(
    id              bigserial PRIMARY KEY,
    created         timestamp,
    creator         varchar(100),
    updated         timestamp,
    updater         varchar(100),
    vietqr_order_id varchar(13)  NOT NULL UNIQUE,
    account_id      bigint       NOT NULL,
    amount          numeric(19, 2) NOT NULL,
    content         varchar(23)  NOT NULL,
    status          varchar(16)  NOT NULL,
    request_json    text         NOT NULL,
    qr_link         varchar(512),
    qr_code         text,
    expires_at      timestamp    NOT NULL
);

alter table checkout_pending owner to dragun;

CREATE TABLE IF NOT EXISTS payment_transaction_log
(
    id                bigserial PRIMARY KEY,
    created           timestamp,
    creator           varchar(100),
    updated           timestamp,
    updater           varchar(100),
    payment_method    varchar(32)  NOT NULL,
    event_type        varchar(64)  NOT NULL,
    status            varchar(16)  NOT NULL,
    vietqr_order_id   varchar(13),
    order_id          bigint,
    order_code        varchar(32),
    account_id        bigint,
    external_txn_id   varchar(64),
    reference_number  varchar(64),
    amount            numeric(19, 2),
    error_code        varchar(64),
    error_message     varchar(500),
    request_payload   text,
    response_payload  text,
    http_status       integer,
    duration_ms       bigint
);

CREATE INDEX IF NOT EXISTS idx_payment_log_vietqr_order_id ON payment_transaction_log (vietqr_order_id);
CREATE INDEX IF NOT EXISTS idx_payment_log_order_code ON payment_transaction_log (order_code);
CREATE INDEX IF NOT EXISTS idx_payment_log_created ON payment_transaction_log (created DESC);

alter table payment_transaction_log owner to dragun;

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

alter table vietqr_pilot_settings owner to dragun;

CREATE TABLE IF NOT EXISTS inventory_reservation
(
    id           bigserial PRIMARY KEY,
    created      timestamp,
    creator      varchar(100),
    updated      timestamp,
    updater      varchar(100),
    order_id     bigint      NOT NULL REFERENCES orders (id),
    variation_id bigint      NOT NULL REFERENCES variations (id),
    quantity     integer     NOT NULL CHECK (quantity > 0),
    status       varchar(16) NOT NULL,
    expires_at   timestamp   NOT NULL,
    stock_deducted boolean   NOT NULL DEFAULT false,
    CONSTRAINT uk_inventory_reservation_order_variation UNIQUE (order_id, variation_id)
);

CREATE INDEX IF NOT EXISTS idx_inventory_reservation_active
    ON inventory_reservation (variation_id, status, expires_at);

CREATE INDEX IF NOT EXISTS idx_inventory_reservation_order
    ON inventory_reservation (order_id);

alter table inventory_reservation owner to dragun;
