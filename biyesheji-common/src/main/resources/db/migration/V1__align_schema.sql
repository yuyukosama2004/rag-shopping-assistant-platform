-- Align databases created by older init.sql files with the current entity mappings.
ALTER TABLE t_product
    ADD COLUMN IF NOT EXISTS color_options JSON DEFAULT NULL COMMENT '可选颜色JSON数组',
    ADD COLUMN IF NOT EXISTS storage_options JSON DEFAULT NULL COMMENT '可选存储JSON数组';

ALTER TABLE t_shopping_cart
    ADD COLUMN IF NOT EXISTS selected_color VARCHAR(50) DEFAULT NULL COMMENT '已选颜色',
    ADD COLUMN IF NOT EXISTS selected_storage VARCHAR(50) DEFAULT NULL COMMENT '已选存储规格';
