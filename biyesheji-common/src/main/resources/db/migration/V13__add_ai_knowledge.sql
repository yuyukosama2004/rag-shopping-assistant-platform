CREATE TABLE t_ai_knowledge (
    id BIGINT NOT NULL AUTO_INCREMENT,
    category VARCHAR(32) NOT NULL,
    title VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_ai_knowledge_status_sort (status, sort_order, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Merchant-maintained AI knowledge';
