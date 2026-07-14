CREATE TABLE t_product_catalog (
    id BIGINT NOT NULL AUTO_INCREMENT,
    catalog_type VARCHAR(16) NOT NULL,
    name VARCHAR(50) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_catalog_type_name (catalog_type, name),
    KEY idx_product_catalog_type_status_sort (catalog_type, status, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品品牌与分类目录';

INSERT INTO t_product_catalog (catalog_type, name, sort_order, status)
SELECT 'BRAND', brand, 0, 1
FROM (SELECT DISTINCT brand FROM t_product WHERE brand IS NOT NULL AND brand <> '') source;

INSERT INTO t_product_catalog (catalog_type, name, sort_order, status)
SELECT 'CATEGORY', category, 0, 1
FROM (SELECT DISTINCT category FROM t_product WHERE category IS NOT NULL AND category <> '') source;
