-- Align databases created by older init.sql files with the current entity mappings.
-- MySQL 8 does not support ADD COLUMN IF NOT EXISTS, so query the catalog and
-- execute the DDL only where it is needed. This keeps the migration compatible
-- with both fresh and pre-existing schemas.
SET @schema_name = DATABASE();

SET @sql = IF(
    EXISTS (SELECT 1 FROM information_schema.columns
            WHERE table_schema = @schema_name AND table_name = 't_product' AND column_name = 'color_options'),
    'SELECT 1',
    'ALTER TABLE t_product ADD COLUMN color_options JSON DEFAULT NULL'
);
PREPARE statement FROM @sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @sql = IF(
    EXISTS (SELECT 1 FROM information_schema.columns
            WHERE table_schema = @schema_name AND table_name = 't_product' AND column_name = 'storage_options'),
    'SELECT 1',
    'ALTER TABLE t_product ADD COLUMN storage_options JSON DEFAULT NULL'
);
PREPARE statement FROM @sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @sql = IF(
    EXISTS (SELECT 1 FROM information_schema.columns
            WHERE table_schema = @schema_name AND table_name = 't_shopping_cart' AND column_name = 'selected_color'),
    'SELECT 1',
    'ALTER TABLE t_shopping_cart ADD COLUMN selected_color VARCHAR(50) DEFAULT NULL'
);
PREPARE statement FROM @sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @sql = IF(
    EXISTS (SELECT 1 FROM information_schema.columns
            WHERE table_schema = @schema_name AND table_name = 't_shopping_cart' AND column_name = 'selected_storage'),
    'SELECT 1',
    'ALTER TABLE t_shopping_cart ADD COLUMN selected_storage VARCHAR(50) DEFAULT NULL'
);
PREPARE statement FROM @sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;
