# SKU 库存迁移与回滚手册

## 迁移范围

Flyway V5 创建 `t_product_sku` 和 `t_stock_ledger`，并把库存关联键迁移到 `sku_id`；
V6 为旧商品生成稳定的负数 ID `LEGACY-<productId>` SKU，同时回填库存、购物车和历史订单项。
旧商品、价格、库存和历史订单快照不会被删除。

截至 `v0.8.0-alpha.2`，目标服务器的 Flyway schema 版本为 V17；历史表的 `installed_rank=18`
包含一条 baseline 记录，不能把它误写为 schema V18。INV-01 新增 V18，为库存流水增加可空的
`event_key`、`before_total/after_total` 和 `before_locked/after_locked`，旧流水和旧应用均可忽略
这些新增列。V7–V18 均保持对 V5/V6 SKU 结构的向后兼容。数据库迁移仍只向前执行，应用回滚
不能替代数据库备份恢复。

## 升级前

1. 执行 `./start.sh backup`，确认加密归档与 `.sha256` 文件均存在。
2. 保存当前 Git tag、应用镜像 tag 和 Flyway 版本。
3. 在维护窗口执行升级，不允许升级期间继续接收新订单。

## 升级后校验

在 MySQL 容器内执行：

```bash
docker exec -i -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" biyesheji-mysql \
  mysql -uroot biyesheji < scripts/verify-sku-migration.sql
```

脚本最后四项必须全部为 `0`。随后抽查旧商品、旧购物车、历史订单，以及一个新 SKU
的库存调整和下单流程。

升级到 V18 后还必须核对：

- `flyway_schema_history` 的最高成功版本是 `18`，脚本为
  `V18__add_stock_ledger_event_snapshots.sql`。
- 一笔成功订单存在 `ORDER_RESERVE` 和 `ORDER_CONFIRM` 流水；取消或超时订单存在
  `ORDER_RESERVE` 和 `ORDER_RELEASE` 流水。
- `event_key` 格式为 `<action>:<orderNo>:<skuId>` 且唯一。
- 每条新订单流水满足 `total = locked + available`，并且前后快照能解释对应库存变化。
- 重放相同事件不会产生第二条流水，也不会再次修改 MySQL 或 Redis 库存。

## 回滚

Flyway 迁移只向前执行，不通过 `DROP COLUMN` 伪造数据库降级。若迁移校验或业务抽查失败：

```bash
RESTORE_CONFIRM=RESTORE_BIYESHEJI_DATA ./start.sh restore backups/data/<升级前备份>.tar.gz.enc
./start.sh rollback <升级前应用-tag>
```

`restore` 会校验 SHA-256、恢复升级前数据库和商品媒体，再启动服务并等待健康检查；
`rollback` 恢复匹配的后端镜像与前端产物。不要只回退应用而保留未验证的新数据库，也不要修改
已执行的 V5/V6 文件。恢复后重新运行旧版本冒烟测试，并保留失败迁移的日志和备份供排查。

V18 只增加可空列和唯一索引，旧应用可以在保留 V18 schema 的情况下启动；但旧应用不会为新的
订单库存动作写完整流水。如果回滚原因涉及库存或流水正确性，必须恢复升级前备份，不能只回退
应用后继续接单。
