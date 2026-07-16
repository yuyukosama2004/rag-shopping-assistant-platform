CREATE TABLE t_order_notification_outbox (
    id BIGINT NOT NULL,
    event_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(48) NOT NULL,
    order_no VARCHAR(64) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_error VARCHAR(500) DEFAULT NULL,
    delivered_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_notification_event (event_id),
    KEY idx_order_notification_pending (status, next_attempt_at),
    KEY idx_order_notification_order (order_no, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Durable order webhook outbox';
