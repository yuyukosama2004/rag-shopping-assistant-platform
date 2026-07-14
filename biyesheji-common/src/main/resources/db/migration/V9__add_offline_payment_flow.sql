SET @schema_name = DATABASE();

SET @sql = IF(
    EXISTS (SELECT 1 FROM information_schema.columns
            WHERE table_schema = @schema_name AND table_name = 't_order' AND column_name = 'payment_method'),
    'SELECT 1',
    'ALTER TABLE t_order ADD COLUMN payment_method VARCHAR(16) NOT NULL DEFAULT ''OFFLINE'' AFTER total_amount'
);
PREPARE statement FROM @sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @sql = IF(
    EXISTS (SELECT 1 FROM information_schema.columns
            WHERE table_schema = @schema_name AND table_name = 't_order' AND column_name = 'processing_at'),
    'SELECT 1',
    'ALTER TABLE t_order ADD COLUMN processing_at DATETIME DEFAULT NULL AFTER pay_time'
);
PREPARE statement FROM @sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;
