CREATE TABLE t_ai_setting (
    id BIGINT NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    model VARCHAR(100) NOT NULL DEFAULT 'deepseek-v4-flash',
    temperature DECIMAL(3,2) NOT NULL DEFAULT 0.70,
    max_output_tokens INT NOT NULL DEFAULT 1200,
    per_user_daily_limit INT NOT NULL DEFAULT 30,
    disclaimer VARCHAR(500) DEFAULT NULL,
    system_prompt TEXT DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Merchant AI settings';

INSERT INTO t_ai_setting
    (id, enabled, model, temperature, max_output_tokens, per_user_daily_limit, disclaimer, system_prompt)
VALUES
    (1, 1, 'deepseek-v4-flash', 0.70, 1200, 30,
     'AI 回答仅供选购参考，商品价格、库存和售后政策以商城页面及商家确认为准。',
     '你是本店的 AI 导购。回答应简洁、诚实，只依据系统提供的商品和商家知识；信息不足时请建议联系商家。');
