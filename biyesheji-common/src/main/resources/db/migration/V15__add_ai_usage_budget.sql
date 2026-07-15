ALTER TABLE t_ai_setting
    ADD COLUMN daily_budget DECIMAL(10,2) NOT NULL DEFAULT 5.00 AFTER per_user_daily_limit,
    ADD COLUMN input_price_per_million DECIMAL(10,4) NOT NULL DEFAULT 2.0000 AFTER daily_budget,
    ADD COLUMN output_price_per_million DECIMAL(10,4) NOT NULL DEFAULT 8.0000 AFTER input_price_per_million,
    ADD COLUMN blocked_keywords VARCHAR(1000) DEFAULT NULL AFTER output_price_per_million;

CREATE TABLE t_ai_request_log (
    id BIGINT NOT NULL,
    model VARCHAR(100) NOT NULL,
    status VARCHAR(16) NOT NULL,
    input_chars INT NOT NULL DEFAULT 0,
    output_chars INT NOT NULL DEFAULT 0,
    estimated_input_tokens INT NOT NULL DEFAULT 0,
    estimated_output_tokens INT NOT NULL DEFAULT 0,
    estimated_cost DECIMAL(12,6) NOT NULL DEFAULT 0,
    duration_ms BIGINT NOT NULL DEFAULT 0,
    failure_reason VARCHAR(200) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_ai_request_created_status (created_at, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Privacy-minimized AI request metrics';
