ALTER TABLE t_stock_ledger
    ADD COLUMN event_key VARCHAR(128) DEFAULT NULL COMMENT '幂等库存事件键' AFTER action,
    ADD COLUMN before_total INT DEFAULT NULL COMMENT '变更前总库存' AFTER quantity,
    ADD COLUMN after_total INT DEFAULT NULL COMMENT '变更后总库存' AFTER before_total,
    ADD COLUMN before_locked INT DEFAULT NULL COMMENT '变更前锁定库存' AFTER after_total,
    ADD COLUMN after_locked INT DEFAULT NULL COMMENT '变更后锁定库存' AFTER before_locked,
    ADD UNIQUE KEY uk_stock_ledger_event_key (event_key);
