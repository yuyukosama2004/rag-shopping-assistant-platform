SET @schema_name = DATABASE();

SET @sql = IF(
    EXISTS (SELECT 1 FROM information_schema.columns
            WHERE table_schema = @schema_name AND table_name = 't_order' AND column_name = 'merchant_note'),
    'SELECT 1',
    'ALTER TABLE t_order ADD COLUMN merchant_note VARCHAR(500) DEFAULT NULL COMMENT ''商家内部备注'' AFTER tracking_no'
);
PREPARE statement FROM @sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;
