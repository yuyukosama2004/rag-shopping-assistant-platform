# AI 经营与库存助手实施计划

> 制定日期：2026-07-18
>
> 产品决策：已确认
>
> 文档状态：待实施
>
> 制定时基线：`v0.8.0-alpha.3`，提交 `5fed019`
>
> 适用产品：单商家、单店铺、单实例、自托管 PhoneMall

## 1. 目标与产品定位

PhoneMall 保留现有面向消费者的 RAG 智能导购，并在此基础上形成两个互补入口：

1. **消费者 AI 购物与售后助手**
   - 根据预算、用途和偏好推荐当前真实可售商品。
   - 回答商家维护的店铺、配送、售后和常见问题。
   - 在严格校验用户身份后查询本人订单与物流。
   - 无法确定时明确提示联系商家，不编造政策或订单状态。
2. **商家 AI 经营与库存助手**
   - 基于 SKU、库存、订单、库存流水生成确定性经营指标。
   - 识别缺货、低库存、健康、超储、慢销和滞销 SKU。
   - 用 AI 解释结构化指标并给出可追溯建议。
   - 支持盘点任务、差异确认和库存调整审计。

本阶段的核心成果不是再增加一个聊天框，而是建立“业务事实计算 → 风险分类 → AI 解释 →
商家确认 → 审计记录”的闭环。

## 2. 成功标准

达到以下条件后，才能认为本阶段完成：

- 同一份业务数据重复计算得到相同指标和风险分类。
- 每条建议都展示时间窗口、销量、库存、最近销售时间和命中规则。
- AI 不自行计算或改写业务数字，只解释服务端提供的结构化事实。
- AI 不直接修改库存、价格、上下架状态、订单或退款。
- 所有库存调整必须由店主或店员显式确认，并写入库存流水和操作审计。
- 订单咨询只能读取当前登录消费者自己的订单，不把地址、手机号等隐私写入向量库。
- AI 不可用时，确定性库存分析和原有商城交易链路仍可独立工作。
- 后端、前端、数据库迁移、权限、完整 E2E、评测和安全扫描全部通过。
- 至少一家试点商家对库存风险结果完成现场复核，并记录误报、漏报和建议采纳情况。

## 3. 当前基线

### 3.1 已有能力

| 领域 | 当前能力 |
| --- | --- |
| 消费者 AI | SSE 对话、商品向量检索、可售过滤、候选约束、历史对话 |
| 商家 AI | 模型、温度、输出长度、每日次数、预算、敏感词、系统提示词和免责声明配置 |
| 商家知识 | FAQ、配送、售后、店铺说明，可启停和排序 |
| AI 质量 | 只读检索评测端点、确定性离线评测、基线比较和回归门禁 |
| 商品 | 商品、SKU、价格、上下架、媒体、CSV 导入导出 |
| 库存 | `total/locked/available`、低库存筛选、人工调整、初始化/人工调整流水 |
| 订单 | SKU 订单明细、收款确认、接单、发货、完成、取消、超时和退款记录 |
| 运维 | GitHub CI、Docker 部署、备份恢复、回滚、Prometheus 和 Alertmanager |

### 3.2 已确认缺口

| 缺口 | 影响 | 本计划处理 |
| --- | --- | --- |
| 库存页只支持低库存阈值 | 无法识别积压、慢销和滞销 | 阶段 2 |
| 订单预留、释放、确认扣减未写入 `t_stock_ledger` | 库存变化原因不完整，盘点差异难追踪 | 阶段 1 |
| 库存流水只记录可用库存前后值 | 确认扣减会改变 `total/locked`，现有字段不能完整表达 | 阶段 1 |
| AI 请求日志没有用途字段 | 无法区分消费者对话和商家经营分析成本 | 阶段 3 |
| 没有采购批次、采购成本和库龄 | 不能可靠计算毛利、资金占用或建议具体降价幅度 | 暂不伪造；阶段 5 后再评估 |
| 没有盘点任务和盘点明细 | 当前只能直接手工调整，缺少盘点快照、差异和复核 | 阶段 4 |
| AI 客服不能查询本人订单 | 只能回答知识库内容，不能称为完整客服 | 阶段 5 |
| 离线评测数据仍待人工标注 | 当前基线证明评测管线，不等于证明真实推荐质量 | 阶段 6 |

## 4. 范围与非目标

### 4.1 本轮范围

- SKU 维度确定性库存健康指标。
- 缺货、低库存、健康、超储、慢销、滞销分类。
- 商家库存洞察页面、证据抽屉和筛选。
- AI 经营总结和单 SKU 解释。
- 盘点任务、盘点明细、差异确认和流水审计。
- 消费者订单/物流只读咨询。
- 对上述能力的测试、评测、指标和运维说明。

### 4.2 明确不做

- AI 自动改价、自动补货、自动下架或自动调整库存。
- 在缺少采购成本时计算利润、GMROI 或具体最低促销价。
- 在缺少采购批次时声称提供精确库龄或 FIFO/FEFO。
- 自动采购、供应商管理、仓库硬件、RFID 或计算机视觉盘点。
- 微信、支付宝、银行卡在线支付和渠道对账。
- 多商家平台、跨商户数据隔离和平台管理员。
- 把订单、地址、手机号、Token 或密钥写入向量库或评测报告。

## 5. 总体架构决策

### 5.1 确定性优先

销量、库存天数、风险等级和盘点差异必须由 SQL/Java 计算。大模型只接收已经计算完成的结构化
JSON，用于解释、归纳和生成可读建议。

### 5.2 默认只读

经营助手默认只有查询能力。任何会改变业务状态的动作继续调用现有受权限保护的业务接口，并要求
商家在独立确认界面操作。不得让模型直接执行数据库写入或拼接任意 SQL。

### 5.3 服务归属

第一版将库存洞察放在 `order-service`：

- 现有 AI、订单和 AI 预算都在 `order-service`。
- 当前三个业务服务共用同一 MySQL，`order-service` 已直接读取商品、SKU 和库存。
- 新接口可复用网关现有 `/api/merchant/ai/**` 路由。
- 可避免在 `product-service` 重复实现模型调用、预算和内容过滤。

新增包建议：

```text
order-service/src/main/java/com/biyesheji/order/
├── controller/MerchantAiInventoryController.java
├── dto/InventoryInsightQuery.java
├── dto/InventoryInsightExplainRequest.java
├── mapper/InventoryInsightMapper.java
├── service/InventoryInsightService.java
├── service/AiInventoryNarrativeService.java
└── service/impl/
```

如果未来拆分独立数据库，再通过订单事件或分析数据集市解耦；本轮不提前引入消息平台或新微服务。

### 5.4 前端入口

新增 `/merchant/ai-operations`，导航名称为“AI 经营助手”。原 `/ai` 消费者页面后续改名为
“AI 购物与售后助手”，但保留现有路由兼容。

## 6. 指标与分类口径

### 6.1 时间与订单口径

- 统计时区：应用配置时区，目标环境固定为 `Asia/Shanghai`。
- 确认销售：`t_order.pay_time IS NOT NULL`。
- 需求订单：排除 `CANCELLED/TIMEOUT`，单独展示，不替代确认销售。
- SKU 销量来源：`t_order_item.quantity`，不能使用 `t_product.sales` 作为唯一事实。
- 确认销售时间使用 `pay_time`；最近下单时间可另用 `created_at`。
- COD 订单发货但尚未确认收款时，可计入履约需求，不计入确认销售额。
- 退款数量在当前模型中不能可靠回冲 SKU 销量，第一版必须在页面标注“未扣除退款数量”；
  后续若增加退款明细数量，再纳入净销量。

### 6.2 SKU 指标

| 字段 | 计算方式 |
| --- | --- |
| `available` | `t_stock.available` |
| `locked` | `t_stock.locked` |
| `total` | `t_stock.total` |
| `confirmedQty7d/30d/90d` | 对应窗口内 `pay_time` 非空订单明细数量之和 |
| `demandQty30d` | 近 30 天排除取消/超时订单的明细数量之和 |
| `confirmedRevenue30d` | 近 30 天确认收款的订单明细 `subtotal` 之和 |
| `dailyVelocity30d` | `confirmedQty30d / 30` |
| `daysOfCover` | `available / dailyVelocity30d`；销量为 0 时返回 `null`，另标记无销量 |
| `lastConfirmedSaleAt` | SKU 最近一笔 `pay_time` 非空订单的收款时间 |
| `daysSinceLastSale` | 当前日期减 `lastConfirmedSaleAt` |
| `skuAgeDays` | 当前日期减 SKU `created_at` |
| `sellThrough90d` | `confirmedQty90d / (confirmedQty90d + available)`；分母为 0 时返回 `null` |

所有小数统一由后端按明确精度计算，前端只负责格式化展示。

### 6.3 风险分类

分类按以下优先级命中第一条，默认阈值先写入应用配置，不建立过早的规则编辑器：

| 优先级 | 风险 | 默认规则 |
| ---: | --- | --- |
| 1 | `OUT_OF_STOCK` | 销售中且 `available = 0` |
| 2 | `LOW_STOCK` | 销售中、`0 < available <= 5` 且近 30 天有确认销量 |
| 3 | `DEAD_STOCK` | `available > 0`、SKU 上线至少 30 天且近 90 天确认销量为 0 |
| 4 | `SLOW_MOVING` | `available > 0`，近 30 天确认销量为 0，但近 90 天有销量 |
| 5 | `OVERSTOCK` | `available >= 10` 且 `daysOfCover > 90` |
| 6 | `HEALTHY` | 未命中上述规则 |

配置建议：

```yaml
inventory:
  insight:
    low-stock-threshold: 5
    dead-stock-min-age-days: 30
    dead-stock-window-days: 90
    overstock-min-available: 10
    overstock-cover-days: 90
```

任何阈值调整都必须更新测试数据和评测基线。

## 7. API 设计

### 7.1 库存洞察列表

```text
GET /api/merchant/ai/inventory-insights
```

查询参数：

```text
pageNum=1
pageSize=20
keyword=
risk=DEAD_STOCK
sort=RISK_DESC
```

响应记录至少包含：

```json
{
  "productId": 1,
  "productName": "示例商品",
  "skuId": 1001,
  "skuCode": "PHONE-BLUE-256",
  "skuSpecJson": "{\"颜色\":\"蓝色\",\"容量\":\"256GB\"}",
  "productStatus": 1,
  "skuStatus": 1,
  "total": 42,
  "locked": 0,
  "available": 42,
  "confirmedQty7d": 0,
  "confirmedQty30d": 2,
  "confirmedQty90d": 4,
  "demandQty30d": 3,
  "confirmedRevenue30d": 5998.00,
  "dailyVelocity30d": 0.0667,
  "daysOfCover": 630.0,
  "lastConfirmedSaleAt": "2026-07-01T10:20:00",
  "daysSinceLastSale": 17,
  "skuAgeDays": 180,
  "sellThrough90d": 0.0870,
  "risk": "OVERSTOCK",
  "ruleCode": "AVAILABLE_GTE_10_AND_COVER_GT_90",
  "calculatedAt": "2026-07-18T12:00:00+08:00"
}
```

### 7.2 汇总

```text
GET /api/merchant/ai/inventory-insights/summary
```

返回各风险数量、总可用库存、近 30 天确认销量、无销量库存数和计算时间。第一版不返回库存成本或
预计利润。

### 7.3 证据明细

```text
GET /api/merchant/ai/inventory-insights/{skuId}/evidence
```

返回：

- 指标及规则。
- 最近确认销售记录的脱敏摘要。
- 最近库存流水。
- 数据限制说明。

不得返回消费者姓名、手机号、地址、Token 或完整订单内部备注。

### 7.4 AI 解释

```text
POST /api/merchant/ai/inventory-insights/explain
```

请求只接受 SKU ID，最多 20 个：

```json
{
  "skuIds": [1001, 1002],
  "focus": "CLEARANCE"
}
```

服务端必须重新查询指标，不能信任客户端传入的销量、库存或金额。响应同时返回确定性事实和 AI 文本：

```json
{
  "facts": [],
  "narrative": "……",
  "generatedAt": "2026-07-18T12:00:00+08:00",
  "model": "deepseek-v4-flash",
  "disclaimer": "建议仅供经营决策参考，执行前请核对库存和成本。"
}
```

AI 失败时返回 `facts` 和可读的规则模板说明，不能让整个接口不可用。

### 7.5 盘点

```text
POST /api/merchant/stocktakes
GET  /api/merchant/stocktakes
GET  /api/merchant/stocktakes/{id}
PUT  /api/merchant/stocktakes/{id}/items/{skuId}
POST /api/merchant/stocktakes/{id}/complete
POST /api/merchant/stocktakes/{id}/cancel
```

完成盘点必须：

- 验证任务仍为进行中。
- 对每个 SKU 使用当前版本号或锁完成并发保护。
- `countedTotal` 不得小于当前 `locked`；否则拒绝并要求先处理订单。
- 把差异写入库存流水，`reference_no` 使用盘点单号。
- 写入商家审计日志。
- 重复完成请求保持幂等。

### 7.6 消费者订单咨询

继续使用：

```text
GET /api/order/ai/chat
```

由服务端识别订单/物流意图，并在查询时强制使用 JWT 中的 `userId`。第一版只提供：

- 最近订单列表摘要。
- 指定本人订单的状态、收款方式、承运商和物流单号。
- 已有退款申请状态。

AI 只能解释只读结果，不得代替用户取消订单、确认收货或提交退款。

## 8. 数据库迁移

迁移编号以实施时 `main` 的最高 Flyway 版本为准；制定本计划时最高为 V18。

### 8.1 库存流水增强

建议下一迁移增加：

```text
t_stock_ledger.event_key            VARCHAR(128) NULL UNIQUE
t_stock_ledger.before_total         INT NULL
t_stock_ledger.after_total          INT NULL
t_stock_ledger.before_locked        INT NULL
t_stock_ledger.after_locked         INT NULL
```

旧记录允许为 `NULL`。新订单库存动作使用：

- `ORDER_RESERVE`
- `ORDER_RELEASE`
- `ORDER_CONFIRM`
- `STOCKTAKE_ADJUST`

`event_key` 示例：

```text
ORDER_RESERVE:<orderNo>:<skuId>
ORDER_RELEASE:<orderNo>:<skuId>
ORDER_CONFIRM:<orderNo>:<skuId>
STOCKTAKE_ADJUST:<stocktakeNo>:<skuId>
```

唯一键用于阻止重试产生重复流水。库存写入与流水写入必须处于同一数据库事务。

### 8.2 AI 请求用途

为 `t_ai_request_log` 增加：

```text
purpose VARCHAR(32) NOT NULL DEFAULT 'CUSTOMER_CHAT'
```

建议值：

- `CUSTOMER_CHAT`
- `PRODUCT_ENRICH`
- `MERCHANT_INVENTORY_EXPLAIN`
- `MERCHANT_CUSTOMER_SERVICE`

Prometheus 请求和费用指标增加低基数 `purpose` 标签，禁止以用户 ID、SKU 或订单号作为指标标签。

### 8.3 盘点表

```text
t_stocktake
  id
  stocktake_no
  status
  note
  created_by
  started_at
  completed_at
  created_at
  updated_at

t_stocktake_item
  id
  stocktake_id
  sku_id
  snapshot_total
  snapshot_locked
  snapshot_available
  counted_total
  variance
  note
  counted_by
  counted_at
```

约束：

- `stocktake_no` 唯一。
- `(stocktake_id, sku_id)` 唯一。
- 状态只允许 `IN_PROGRESS/COMPLETED/CANCELLED`。
- 完成后盘点明细不可修改。
- 不删除历史盘点记录。

### 8.4 查询索引

使用 `EXPLAIN` 验证后再添加，候选包括：

```text
t_order(pay_time, status, id)
t_order_item(sku_id, order_id)
```

不得仅凭猜测添加重复索引。迁移测试需覆盖全新数据库和 V18 升级路径。

## 9. 分阶段实施

### 阶段 0：基线与决策冻结

任务：

- [ ] 确认最新 `main`、Release、CI 和服务器状态。
- [ ] 建立独立分支，不直接修改 `main`。
- [ ] 把本计划拆分为 GitHub Issues。
- [ ] 固定上述指标、风险优先级和订单口径。
- [ ] 准备确定性测试数据：畅销、低库存、超储、慢销、滞销、无订单、COD 未收款、取消和超时。
- [ ] 明确第一版不计算采购成本、利润和精确库龄。

验收：

- 指标样例由人工手算并形成测试夹具。
- 所有参与开发的人对销售口径和风险优先级无歧义。

### 阶段 1：库存事实链与数据正确性

任务：

- [ ] 扩展库存流水字段和 `event_key`。
- [ ] 在库存预留前生成 `orderNo`，并把它传给库存服务作为稳定业务引用。
- [ ] 订单预留、释放、确认扣减写入流水。
- [ ] 保留初始化和人工调整流水兼容。
- [ ] 为库存动作增加幂等与事务测试。
- [ ] 验证 Redis 与 MySQL 的 `total/locked/available` 不变量。
- [ ] 增加数据库升级和应用回滚兼容测试。

验收：

- 一笔订单可完整看到预留和确认，取消/超时可看到预留和释放。
- 重复调用不会产生重复流水或重复扣减。
- `total >= locked >= 0`、`available >= 0`、`total = locked + available` 在所有测试后成立。

### 阶段 2：确定性库存洞察

任务：

- [ ] 新增 `InventoryInsightMapper` 和聚合查询。
- [ ] 实现指标、风险分类和稳定排序。
- [ ] 实现列表、汇总和证据接口。
- [ ] 增加 OWNER/STAFF 权限与 CUSTOMER 拒绝测试。
- [ ] 使用 Testcontainers MySQL 验证时间窗口和金额精度。
- [ ] 使用 `EXPLAIN` 检查查询计划。

验收：

- 测试夹具分类与人工预期完全一致。
- 相同输入重复调用结果一致，只有 `calculatedAt` 可变化。
- 无 AI Key 时所有洞察接口正常。
- 证据响应不包含消费者隐私。

### 阶段 3：商家页面与 AI 解释

任务：

- [ ] 新增“AI 经营助手”路由和导航。
- [ ] 实现风险卡片、筛选表格、排序、证据抽屉和空状态。
- [ ] 移动端和桌面端均可使用。
- [ ] 新增 AI 解释接口，服务端重取事实。
- [ ] AI 请求日志增加 `purpose`。
- [ ] 对模型输出进行长度限制、内容过滤和失败降级。
- [ ] 清楚展示规则说明、时间窗口和免责声明。

验收：

- 页面首次加载不调用大模型。
- 商家点击“生成解读”后才产生 AI 费用。
- 模型断网、超时、预算耗尽时仍展示确定性结果。
- AI 文本中的所有数字能在同一响应的 `facts` 中找到。
- 不出现“自动执行”“保证销量”等误导性承诺。

### 阶段 4：盘点闭环

任务：

- [ ] 新增盘点表、实体、Mapper、Service 和接口。
- [ ] 创建任务时冻结账面快照。
- [ ] 支持逐项录入实际数量和备注。
- [ ] 完成时重新校验当前库存与锁定数量。
- [ ] 差异调整写入库存流水和商家审计。
- [ ] 增加并发、重复提交、取消和完成后不可修改测试。
- [ ] 新增盘点列表、详情和录入页面。

验收：

- 盘点前后库存变化可由盘点明细、库存流水和审计日志三方对应。
- 实盘数量小于锁定数量时不能完成。
- 重复完成不会重复调整库存。
- AI 只能解释差异，不能替商家确认差异。

### 阶段 5：消费者 AI 客服扩展

任务：

- [ ] 将消费者文案调整为“AI 购物与售后助手”。
- [ ] 增加售前、政策、订单/物流和未知意图路由。
- [ ] 订单查询必须绑定 JWT `userId`。
- [ ] 仅向模型提供脱敏且最小化的订单上下文。
- [ ] 售后回答引用商家知识和已有退款状态。
- [ ] 无法回答时显示店铺联系方式或人工处理提示。
- [ ] 增加跨用户订单越权、提示注入和隐私泄漏测试。

验收：

- 用户 A 无法通过问题、订单号或提示注入获得用户 B 的订单信息。
- 未登录用户不能调用订单咨询。
- AI 不执行取消、确认收货、退款或地址修改。
- 商品检索质量不因客服意图扩展而回退。

### 阶段 6：评测、可观测性与安全

任务：

- [ ] 扩展 `evals/`，加入库存分类确定性数据集。
- [ ] 指标至少包括风险分类准确率、证据完整率、意外空结果和禁止写操作率。
- [ ] 人工审核现有导购候选标签，生成真正的 Gold Set。
- [ ] 增加客服越权和敏感数据测试集。
- [ ] 增加按 `purpose` 的请求量、失败率、耗时和费用指标。
- [ ] 为库存洞察接口增加延迟和错误率告警。
- [ ] 更新威胁模型、发布说明、质量验收和试点手册。
- [ ] 将 `evaluation` 加入 `main` 必需检查，消除“执行但非必需”的门禁缺口。

验收：

- PR 和 `main` 的全部 CI 任务通过。
- 评测失败会阻止合并。
- 报告不包含 Token、地址、手机号、完整订单数据或模型密钥。
- 线上关闭 AI 时商城、库存洞察和盘点仍可运行。

### 阶段 7：试点与发布

任务：

- [ ] 选择一家使用非唯一生产数据的试点商家。
- [ ] 让商家独立复核不少于 20 个 SKU 的风险分类。
- [ ] 记录误报、漏报、建议采纳、操作耗时和支持请求。
- [ ] 完成一次盘点任务和差异调整。
- [ ] 完成至少一次消费者订单/物流咨询演练。
- [ ] 演练 AI 不可用、MySQL/Redis 恢复、备份和版本回滚。
- [ ] 关闭 P0/P1 缺陷后再发布新的 Alpha；不因功能完成直接标记稳定版。

Go 条件：

- 无丢单、负库存、跨用户越权或不可恢复数据错误。
- 商家能理解风险规则并独立完成盘点。
- AI 建议没有无法追溯到事实的关键数字。

No-Go 条件：

- 任一库存动作重复执行。
- AI 可绕过确认修改业务数据。
- 客服泄漏其他用户订单或隐私。
- 评测和现场结果明显冲突且原因未查明。

## 10. 测试矩阵

| 层级 | 必测内容 |
| --- | --- |
| 单元测试 | 指标公式、风险优先级、除零、时间边界、AI 模板降级 |
| Mapper/Testcontainers | 聚合 SQL、状态过滤、COD、取消/超时、金额精度、分页和排序 |
| 迁移测试 | V18 升级、新装、旧记录空字段、唯一事件键、回滚兼容 |
| Controller | OWNER/STAFF 成功、CUSTOMER/匿名拒绝、参数上限 |
| 库存并发 | 预留/释放/确认/人工调整/盘点同时发生，不变量保持 |
| 前端组件 | 风险标签、空状态、筛选、证据、AI 失败和预算耗尽 |
| E2E | 建 SKU → 下单 → 确认/取消 → 洞察变化 → AI 解读 → 盘点 |
| 安全 | 越权订单、伪造角色头、提示注入、隐私输出、任意 SKU 批量请求 |
| 评测 | 风险分类、证据字段、无写操作、导购召回不回退 |
| 运维 | AI/Embedding 断网、服务重启、备份恢复、应用回滚 |

## 11. 前端体验要求

- 风险颜色不能是唯一信息，必须同时显示文字和规则。
- 所有指标显示统计窗口和最后计算时间。
- `daysOfCover = null` 显示“近 30 天无确认销量”，不能显示 `∞` 后不解释。
- AI 文本与业务事实分区展示，业务事实优先。
- “生成 AI 解读”按钮显示可能产生费用。
- 操作建议使用“查看商品”“查看流水”“创建盘点”等安全跳转，不提供“一键自动清仓”。
- 移动端表格使用卡片或横向滚动，主要指标无需缩放即可读取。
- 所有加载、超时、无数据、权限不足和 AI 不可用状态有明确提示。

## 12. 安全与隐私要求

- 网关继续清除客户端伪造的身份 Header，并注入可信身份。
- SKU ID 列表限制数量，防止批量数据抓取和超长 Prompt。
- 模型上下文不包含消费者姓名、电话、地址、Token、密钥或商家内部备注。
- AI 解释请求不写入用户对话历史。
- AI 日志只记录用途、字符/Token 估算、费用、耗时和失败原因，不记录完整 Prompt。
- 模型输出按现有敏感词和流式内容过滤策略处理。
- 所有写操作使用现有角色校验、事务、幂等和审计机制。
- 数据导出使用公式注入防护，避免 CSV 单元格以 `=`, `+`, `-`, `@` 开头时被执行。

## 13. 可观测性

建议新增：

```text
biyesheji.inventory.insight.requests{outcome}
biyesheji.inventory.insight.duration
biyesheji.inventory.insight.risk.count{risk}
biyesheji.stocktake.completed
biyesheji.stocktake.variance.quantity
biyesheji.ai.requests{purpose,outcome}
biyesheji.ai.estimated.cost{purpose}
```

禁止 SKU、订单号、用户 ID 等高基数字段作为指标标签。结构化日志中只记录请求 ID、角色、统计窗口、
记录数、耗时和结果，不记录完整模型上下文。

## 14. PR 与发布顺序

每个 PR 保持可独立验证和回滚：

1. `INV-01`：库存流水完整性、事件幂等和迁移。
2. `INV-02`：确定性库存洞察 Mapper、Service、API 和测试。
3. `INV-03`：商家库存洞察页面。
4. `INV-04`：AI 解释、用途计费、降级和安全测试。
5. `STOCKTAKE-01`：盘点数据模型和后端。
6. `STOCKTAKE-02`：盘点前端和 E2E。
7. `CS-01`：消费者订单/物流只读咨询。
8. `EVAL-01`：库存/客服评测、指标和文档。
9. `PILOT-01`：服务器部署、现场验收和新预发布版本。

禁止把所有阶段压入一个大 PR。每个 PR 必须：

- 从最新 `main` 创建 `codex/` 分支。
- 包含相应测试和文档。
- 通过完整 CI。
- 由 PR 合并，不绕过分支保护。

## 15. Definition of Done

每个 Issue 完成时必须满足：

- [ ] 需求、非目标和业务口径已写明。
- [ ] 数据库迁移向前兼容，旧应用回滚边界明确。
- [ ] 服务端不信任客户端传入的业务事实。
- [ ] 权限、幂等、并发和失败路径有自动化测试。
- [ ] 前端有加载、空状态、错误和移动端行为。
- [ ] AI 不可用时有确定性降级。
- [ ] 指标、日志和告警不泄漏敏感数据。
- [ ] 相关评测基线经过人工审查，不通过静默重写基线掩盖回退。
- [ ] PR 和合并后 `main` CI 全绿。
- [ ] 目标服务器部署前已备份，升级后已冒烟，必要时可回滚。
- [ ] 文档反映真实完成状态，不把内部测试写成真实商家验证。

## 16. 延后决策

以下问题等待真实试点数据后决定，不阻塞第一版：

- 是否增加采购单、供应商、采购批次和单位成本。
- 是否根据成本与毛利生成具体促销价建议。
- 是否增加正式客服工单和人工接管队列。
- 是否将库存洞察独立成分析服务或数据集市。
- 是否允许商家自定义风险阈值。
- 是否接入扫码枪、移动盘点或条码打印。

在这些决策之前，不增加预留表、不引入新消息中间件，也不对外承诺相关能力。
