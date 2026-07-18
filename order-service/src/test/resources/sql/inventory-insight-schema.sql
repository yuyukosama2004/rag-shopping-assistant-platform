CREATE TABLE t_product (
    id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE t_product_sku (
    id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    sku_code VARCHAR(64) NOT NULL,
    spec_json VARCHAR(2000) DEFAULT NULL,
    price DECIMAL(10,2) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_sku_code (sku_code),
    KEY idx_product_sku_product (product_id, status)
) ENGINE=InnoDB;

CREATE TABLE t_stock (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    sku_id BIGINT DEFAULT NULL,
    total INT NOT NULL,
    locked INT NOT NULL,
    available INT NOT NULL,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_stock_sku (sku_id)
) ENGINE=InnoDB;

CREATE TABLE t_order (
    id BIGINT NOT NULL,
    order_no VARCHAR(32) NOT NULL,
    user_id BIGINT NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    payment_method VARCHAR(16) NOT NULL DEFAULT 'OFFLINE',
    status TINYINT NOT NULL,
    receiver_name VARCHAR(50) DEFAULT NULL,
    receiver_phone VARCHAR(20) DEFAULT NULL,
    receiver_address VARCHAR(255) DEFAULT NULL,
    merchant_note VARCHAR(500) DEFAULT NULL,
    pay_time DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_status (status)
) ENGINE=InnoDB;

CREATE TABLE t_order_item (
    id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    order_no VARCHAR(32) NOT NULL,
    product_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    sku_code VARCHAR(64) NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    quantity INT NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    KEY idx_order_id (order_id),
    KEY idx_order_item_sku (sku_id)
) ENGINE=InnoDB;

CREATE TABLE t_stock_ledger (
    id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    action VARCHAR(32) NOT NULL,
    event_key VARCHAR(128) DEFAULT NULL,
    quantity INT NOT NULL,
    before_total INT DEFAULT NULL,
    after_total INT DEFAULT NULL,
    before_locked INT DEFAULT NULL,
    after_locked INT DEFAULT NULL,
    before_available INT NOT NULL,
    after_available INT NOT NULL,
    operator_id BIGINT DEFAULT NULL,
    reference_no VARCHAR(64) DEFAULT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_stock_ledger_event_key (event_key),
    KEY idx_stock_ledger_sku_created (sku_id, created_at)
) ENGINE=InnoDB;
