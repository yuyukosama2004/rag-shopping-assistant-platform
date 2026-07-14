CREATE TABLE IF NOT EXISTS t_merchant_audit_log (
    id BIGINT NOT NULL COMMENT '审计记录ID',
    operator_id BIGINT NOT NULL COMMENT '操作者用户ID',
    action VARCHAR(50) NOT NULL COMMENT '动作',
    resource_type VARCHAR(50) NOT NULL COMMENT '对象类型',
    resource_id BIGINT DEFAULT NULL COMMENT '对象ID',
    result VARCHAR(20) NOT NULL COMMENT '操作结果',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (id),
    KEY idx_merchant_audit_operator_created (operator_id, created_at),
    KEY idx_merchant_audit_resource (resource_type, resource_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商家关键操作审计日志';
