CREATE TABLE IF NOT EXISTS t_shipping_rule (
    id BIGINT NOT NULL AUTO_INCREMENT,
    rule_type VARCHAR(16) NOT NULL,
    name VARCHAR(50) NOT NULL,
    base_fee DECIMAL(10,2) NOT NULL DEFAULT 0,
    free_shipping_threshold DECIMAL(10,2) DEFAULT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_shipping_rule_status_sort (status, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Shipping rules';

INSERT INTO t_shipping_rule (rule_type, name, base_fee, status, sort_order)
SELECT 'DELIVERY', 'Standard delivery', 0, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM t_shipping_rule WHERE deleted = 0);

SET @schema_name = DATABASE();
SET @sql = IF(EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 't_order' AND column_name = 'product_amount'), 'SELECT 1', 'ALTER TABLE t_order ADD COLUMN product_amount DECIMAL(10,2) NOT NULL DEFAULT 0 AFTER total_amount');
PREPARE statement FROM @sql; EXECUTE statement; DEALLOCATE PREPARE statement;
SET @sql = IF(EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 't_order' AND column_name = 'shipping_fee'), 'SELECT 1', 'ALTER TABLE t_order ADD COLUMN shipping_fee DECIMAL(10,2) NOT NULL DEFAULT 0 AFTER product_amount');
PREPARE statement FROM @sql; EXECUTE statement; DEALLOCATE PREPARE statement;
SET @sql = IF(EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 't_order' AND column_name = 'shipping_rule_id'), 'SELECT 1', 'ALTER TABLE t_order ADD COLUMN shipping_rule_id BIGINT DEFAULT NULL AFTER shipping_fee');
PREPARE statement FROM @sql; EXECUTE statement; DEALLOCATE PREPARE statement;
SET @sql = IF(EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 't_order' AND column_name = 'shipping_rule_name'), 'SELECT 1', 'ALTER TABLE t_order ADD COLUMN shipping_rule_name VARCHAR(50) DEFAULT NULL AFTER shipping_rule_id');
PREPARE statement FROM @sql; EXECUTE statement; DEALLOCATE PREPARE statement;
SET @sql = IF(EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = @schema_name AND table_name = 't_order' AND column_name = 'shipping_method'), 'SELECT 1', 'ALTER TABLE t_order ADD COLUMN shipping_method VARCHAR(16) DEFAULT NULL AFTER shipping_rule_name');
PREPARE statement FROM @sql; EXECUTE statement; DEALLOCATE PREPARE statement;

UPDATE t_order
SET product_amount = total_amount, shipping_fee = 0
WHERE product_amount = 0 AND total_amount > 0;
