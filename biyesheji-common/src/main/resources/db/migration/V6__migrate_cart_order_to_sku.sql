ALTER TABLE t_shopping_cart ADD COLUMN sku_id BIGINT DEFAULT NULL COMMENT 'SKU标识' AFTER product_id;
ALTER TABLE t_order_item ADD COLUMN sku_id BIGINT DEFAULT NULL COMMENT 'SKU标识' AFTER product_id;
ALTER TABLE t_order_item ADD COLUMN sku_code VARCHAR(64) DEFAULT NULL COMMENT 'SKU编码' AFTER sku_id;
ALTER TABLE t_order_item ADD COLUMN sku_spec_json VARCHAR(2000) DEFAULT NULL COMMENT 'SKU规格快照' AFTER sku_code;

INSERT INTO t_product_sku (id, product_id, sku_code, spec_json, price, original_price, status)
SELECT -p.id, p.id, CONCAT('LEGACY-', p.id), '{}', p.price, p.original_price, p.status
FROM t_product p
WHERE EXISTS (SELECT 1 FROM t_stock s WHERE s.product_id = p.id AND s.sku_id IS NULL)
   OR EXISTS (SELECT 1 FROM t_shopping_cart c WHERE c.product_id = p.id AND c.sku_id IS NULL)
   OR EXISTS (SELECT 1 FROM t_order_item i WHERE i.product_id = p.id AND i.sku_id IS NULL);

UPDATE t_stock SET sku_id = -product_id WHERE sku_id IS NULL;
UPDATE t_shopping_cart SET sku_id = -product_id WHERE sku_id IS NULL;
UPDATE t_order_item
SET sku_id = -product_id,
    sku_code = CONCAT('LEGACY-', product_id),
    sku_spec_json = '{}'
WHERE sku_id IS NULL;

ALTER TABLE t_shopping_cart DROP INDEX uk_user_product;
ALTER TABLE t_shopping_cart ADD UNIQUE KEY uk_user_sku (user_id, sku_id);
ALTER TABLE t_order_item ADD KEY idx_order_item_sku (sku_id);
