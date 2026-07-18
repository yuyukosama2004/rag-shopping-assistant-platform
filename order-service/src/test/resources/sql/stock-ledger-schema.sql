CREATE TABLE t_stock (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    sku_id BIGINT DEFAULT NULL,
    total INT NOT NULL DEFAULT 0,
    locked INT NOT NULL DEFAULT 0,
    available INT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_stock_sku (sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_stock_ledger_event_key (event_key),
    KEY idx_stock_ledger_sku_created (sku_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
