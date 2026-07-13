CREATE TABLE IF NOT EXISTS t_store_setting (
    id BIGINT NOT NULL COMMENT '固定为1，单实例店铺配置',
    store_name VARCHAR(100) NOT NULL COMMENT '店铺名称',
    logo VARCHAR(255) DEFAULT NULL COMMENT 'Logo地址',
    service_phone VARCHAR(20) DEFAULT NULL COMMENT '客服联系电话',
    service_email VARCHAR(100) DEFAULT NULL COMMENT '客服邮箱',
    address VARCHAR(255) DEFAULT NULL COMMENT '店铺地址',
    business_status TINYINT NOT NULL DEFAULT 1 COMMENT '营业状态: 0-休息 1-营业',
    shipping_notice VARCHAR(2000) DEFAULT NULL COMMENT '配送说明',
    after_sales_notice VARCHAR(2000) DEFAULT NULL COMMENT '售后说明',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT ck_store_setting_singleton CHECK (id = 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='单店店铺配置';
