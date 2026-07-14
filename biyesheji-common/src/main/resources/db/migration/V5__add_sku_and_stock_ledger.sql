CREATE TABLE IF NOT EXISTS t_product_sku (
    id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    sku_code VARCHAR(64) NOT NULL,
    spec_json VARCHAR(2000) DEFAULT NULL,
    price DECIMAL(10,2) NOT NULL,
    original_price DECIMAL(10,2) DEFAULT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_sku_code (sku_code),
    KEY idx_product_sku_product (product_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品SKU';

ALTER TABLE t_stock DROP INDEX uk_product_id;
ALTER TABLE t_stock ADD COLUMN sku_id BIGINT DEFAULT NULL COMMENT 'SKU库存标识';
ALTER TABLE t_stock ADD UNIQUE KEY uk_stock_sku (sku_id);

CREATE TABLE IF NOT EXISTS t_stock_ledger (
    id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    action VARCHAR(32) NOT NULL,
    quantity INT NOT NULL,
    before_available INT NOT NULL,
    after_available INT NOT NULL,
    operator_id BIGINT DEFAULT NULL,
    reference_no VARCHAR(64) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_stock_ledger_sku_created (sku_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SKU库存流水';
