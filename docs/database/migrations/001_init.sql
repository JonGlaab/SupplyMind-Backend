-- =========================
-- SUPPLYMIND DATABASE SCHEMA
-- =========================

SET FOREIGN_KEY_CHECKS = 0;

-- -------------------------
-- USERS
-- -------------------------
CREATE TABLE users (
    user_id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    email             VARCHAR(255) NOT NULL UNIQUE,
    password_hash     VARCHAR(255) NOT NULL,
    role              VARCHAR(30) NOT NULL,
    is_2fa_enabled     BOOLEAN DEFAULT FALSE
);

-- -------------------------
-- WAREHOUSES
-- -------------------------
CREATE TABLE warehouses (
    warehouse_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    location_name  VARCHAR(255) NOT NULL,
    address         TEXT,
    capacity        INT
);

-- -------------------------
-- PRODUCTS
-- -------------------------
CREATE TABLE products (
    product_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku             VARCHAR(50) NOT NULL UNIQUE,
    name            VARCHAR(255) NOT NULL,
    category         VARCHAR(100),
    unit_price       DECIMAL(15,2),
    reorder_point    INT
);

-- -------------------------
-- SUPPLIERS
-- -------------------------
CREATE TABLE suppliers (
    supplier_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    contact_email   VARCHAR(255),
    phone            VARCHAR(30),
    address          TEXT
);

-- -------------------------
-- CUSTOMERS
-- -------------------------
CREATE TABLE customers (
    customer_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(255) NOT NULL,
    email          VARCHAR(255),
    address        TEXT
);

-- -------------------------
-- INVENTORY
-- -------------------------
CREATE TABLE inventory (
    inventory_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    warehouse_id   BIGINT NOT NULL,
    product_id     BIGINT NOT NULL,
    qty_on_hand    INT NOT NULL,

    CONSTRAINT fk_inventory_warehouse
        FOREIGN KEY (warehouse_id) REFERENCES warehouses(warehouse_id),

    CONSTRAINT fk_inventory_product
        FOREIGN KEY (product_id) REFERENCES products(product_id),

    UNIQUE (warehouse_id, product_id)
);

-- -------------------------
-- INVENTORY TRANSACTIONS
-- -------------------------
CREATE TABLE inventory_transactions (
    transaction_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id       BIGINT NOT NULL,
    warehouse_id     BIGINT NOT NULL,
    type              VARCHAR(20) NOT NULL,   -- IN, OUT, RETURN
    quantity          INT NOT NULL,
    timestamp         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_it_product
        FOREIGN KEY (product_id) REFERENCES products(product_id),

    CONSTRAINT fk_it_warehouse
        FOREIGN KEY (warehouse_id) REFERENCES warehouses(warehouse_id)
);

-- -------------------------
-- SUPPLIER PRODUCTS
-- -------------------------
CREATE TABLE supplier_products (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    supplier_id     BIGINT NOT NULL,
    product_id      BIGINT NOT NULL,
    lead_time_days  INT,

    CONSTRAINT fk_sp_supplier
        FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id),

    CONSTRAINT fk_sp_product
        FOREIGN KEY (product_id) REFERENCES products(product_id),

    UNIQUE (supplier_id, product_id)
);

-- -------------------------
-- PURCHASE ORDERS
-- -------------------------
CREATE TABLE purchase_orders (
    po_id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    supplier_id    BIGINT NOT NULL,
    warehouse_id   BIGINT NOT NULL,
    buyer_id       BIGINT NOT NULL,
    status          VARCHAR(30) NOT NULL,
    total_amount    DECIMAL(15,2),
    created_on      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_po_supplier
        FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id),

    CONSTRAINT fk_po_warehouse
        FOREIGN KEY (warehouse_id) REFERENCES warehouses(warehouse_id),

    CONSTRAINT fk_po_buyer
        FOREIGN KEY (buyer_id) REFERENCES users(user_id)
);

-- -------------------------
-- PURCHASE ORDER ITEMS
-- -------------------------
CREATE TABLE purchase_order_items (
    po_item_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    po_id           BIGINT NOT NULL,
    product_id      BIGINT NOT NULL,
    ordered_qty     INT NOT NULL,
    received_qty    INT,
    unit_cost       DECIMAL(15,2),

    CONSTRAINT fk_poi_po
        FOREIGN KEY (po_id) REFERENCES purchase_orders(po_id),

    CONSTRAINT fk_poi_product
        FOREIGN KEY (product_id) REFERENCES products(product_id)
);

-- -------------------------
-- PAYMENTS
-- -------------------------
CREATE TABLE payments (
    payment_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    po_id           BIGINT NOT NULL,
    stripe_id       VARCHAR(255),
    amount           DECIMAL(15,2),
    status           VARCHAR(30),
    payment_type     VARCHAR(30),  -- PURCHASE, RETURN
    paid_at          TIMESTAMP,

    CONSTRAINT fk_payment_po
        FOREIGN KEY (po_id) REFERENCES purchase_orders(po_id)
);

-- -------------------------
-- SALES ORDERS
-- -------------------------
CREATE TABLE sales_orders (
    so_id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id    BIGINT NOT NULL,
    warehouse_id   BIGINT NOT NULL,
    status           VARCHAR(30),

    CONSTRAINT fk_so_customer
        FOREIGN KEY (customer_id) REFERENCES customers(customer_id),

    CONSTRAINT fk_so_warehouse
        FOREIGN KEY (warehouse_id) REFERENCES warehouses(warehouse_id)
);

-- -------------------------
-- SALES ORDER ITEMS
-- -------------------------
CREATE TABLE sales_order_items (
    so_item_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    so_id           BIGINT NOT NULL,
    product_id      BIGINT NOT NULL,
    quantity         INT NOT NULL,

    CONSTRAINT fk_soi_so
        FOREIGN KEY (so_id) REFERENCES sales_orders(so_id),

    CONSTRAINT fk_soi_product
        FOREIGN KEY (product_id) REFERENCES products(product_id)
);

-- -------------------------
-- USER SESSIONS
-- -------------------------
CREATE TABLE user_sessions (
    session_id     CHAR(36) PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    mfa_verified     BOOLEAN DEFAULT FALSE,
    expiry_at        TIMESTAMP NOT NULL,

    CONSTRAINT fk_session_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- -------------------------
-- TWO FACTOR AUTH
-- -------------------------
CREATE TABLE two_factor_auth (
    mfa_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT NOT NULL,
    secret_key     VARCHAR(255) NOT NULL,
    backup_codes   TEXT,

    CONSTRAINT fk_mfa_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- -------------------------
-- RETURNS
-- -------------------------
CREATE TABLE returns (
    return_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    po_id           BIGINT NOT NULL,
    reason           TEXT,
    status           VARCHAR(40),
    received_at      TIMESTAMP,

    CONSTRAINT fk_return_po
        FOREIGN KEY (po_id) REFERENCES purchase_orders(po_id)
);

-- -------------------------
-- DEMAND FORECASTING
-- -------------------------
CREATE TABLE demand_forecasting (
    forecast_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id      BIGINT NOT NULL,
    warehouse_id    BIGINT NOT NULL,
    forecasted_qty  INT,
    target_date     DATE,
    conf_score       DECIMAL(5,2),

    CONSTRAINT fk_df_product
        FOREIGN KEY (product_id) REFERENCES products(product_id),

    CONSTRAINT fk_df_warehouse
        FOREIGN KEY (warehouse_id) REFERENCES warehouses(warehouse_id)
);

SET FOREIGN_KEY_CHECKS = 1;
