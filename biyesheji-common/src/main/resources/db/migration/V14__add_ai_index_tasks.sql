CREATE TABLE t_ai_index_task (
    id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    operation VARCHAR(16) NOT NULL,
    product_updated_at DATETIME DEFAULT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    error_message VARCHAR(500) DEFAULT NULL,
    processed_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_ai_index_status_created (status, created_at),
    KEY idx_ai_index_product_created (product_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Trackable product AI index tasks';
