-- Every result must be 0 after Flyway V5/V6 has migrated a legacy database.
SELECT COUNT(*) AS stock_without_sku
FROM t_stock
WHERE sku_id IS NULL;

SELECT COUNT(*) AS cart_without_sku
FROM t_shopping_cart
WHERE sku_id IS NULL;

SELECT COUNT(*) AS order_item_without_sku
FROM t_order_item
WHERE sku_id IS NULL OR sku_code IS NULL;

SELECT COUNT(*) AS missing_sku_reference
FROM (
    SELECT sku_id FROM t_stock
    UNION ALL SELECT sku_id FROM t_shopping_cart
    UNION ALL SELECT sku_id FROM t_order_item
) referenced
LEFT JOIN t_product_sku sku ON sku.id = referenced.sku_id
WHERE sku.id IS NULL;
