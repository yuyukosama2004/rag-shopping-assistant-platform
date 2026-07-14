ALTER TABLE t_order ADD COLUMN shipping_carrier VARCHAR(64) DEFAULT NULL COMMENT '承运商' AFTER receiver_address;
ALTER TABLE t_order ADD COLUMN tracking_no VARCHAR(64) DEFAULT NULL COMMENT '运单号' AFTER shipping_carrier;
ALTER TABLE t_order ADD COLUMN shipped_at DATETIME DEFAULT NULL COMMENT '发货时间' AFTER pay_time;

CREATE TABLE t_order_operation (
    id BIGINT NOT NULL,
    order_no VARCHAR(32) NOT NULL,
    action VARCHAR(32) NOT NULL,
    operator_id BIGINT DEFAULT NULL,
    note VARCHAR(255) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_order_operation_order_created (order_no, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单操作记录';
