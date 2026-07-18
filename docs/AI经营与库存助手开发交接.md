# AI 经营与库存助手开发交接

> 交接日期：2026-07-18
>
> 面向对象：新的 Codex 开发窗口
>
> 当前任务：按《AI 经营与库存助手实施计划》开始分阶段开发
>
> 制定时 GitHub 基线：`v0.8.0-alpha.3` / `5fed019`
>
> 重要：交接文档合并后 `main` 会产生更新的文档提交，实际开发必须以远端最新 `main` 为准。

## 1. 用户已经确认的产品决策

不要再次询问是否要保留智能导购。用户已经同意以下方向：

- 保留消费者 RAG 智能导购。
- 将消费者入口演进为“AI 购物与售后助手”。
- 新增面向商家的“AI 经营与库存助手”。
- 库存运营能力是下一阶段主功能，智能客服是后续自然扩展。
- AI 默认只读，不自动修改价格、库存、上下架、订单或退款。
- 先做确定性业务指标，再使用 AI 解释。
- 盘点必须由商家录入实物数量并确认，不能宣称 AI 自动知道实物库存。

权威实施范围见 [`AI经营与库存助手实施计划.md`](AI经营与库存助手实施计划.md)。

## 2. 仓库与发布基线

| 项目 | 当前信息 |
| --- | --- |
| GitHub | `yuyukosama2004/rag-shopping-assistant-platform` |
| 可见性 | Public |
| 默认分支 | `main` |
| 制定交接时 `main` | `5fed019` |
| 最新发布 | `v0.8.0-alpha.3`，预发布版 |
| Alpha 3 内容 | 只读 AI 检索评测端点、确定性 Eval42 Core、离线基线和回归任务 |
| 最新已查 CI | `29498604745`，`5fed019`，通过 |
| CI 工作 | 14 个，包括 `evaluation` |
| 分支保护 | 当前仍只要求原 13 个上下文；`evaluation` 会运行但尚未加入 required contexts |

开始开发前执行：

```powershell
git fetch origin
git status -sb
git log -5 --oneline
gh release list --limit 5
gh run list --workflow CI --branch main --limit 3
```

不要假设上表中的提交仍是最新提交。

## 3. 开发位置与服务器状态

用户此前明确要求：

- 后续主要开发直接在服务器 `/home/jill3/project/biyesheji` 进行。
- 本机 `D:\项目\rag-shopping-assistant-platform` 只用于辅助检查和 GitHub 操作。
- 不要未经说明把主要代码开发重新搬回本机。

服务器：

```text
Host: 192.168.31.39
User: jill3
Project: /home/jill3/project/biyesheji
```

连接命令：

```powershell
ssh jill3@192.168.31.39
```

2026-07-18 制定交接文档时，从本机直连 `192.168.31.39:22` 超时。最近一次已验证状态是
2026-07-16：

- 服务器仓库已同步到 `2f0ecb4`。
- 运行中的四个应用镜像为 `v0.8.0-alpha.2` 且健康。
- 工作树只存在未跟踪的 `backups/`。

此后 GitHub 已发布 `v0.8.0-alpha.3`，但本次无法确认服务器是否升级。新窗口第一件事是恢复 SSH
并重新核对，不能把“最近已知状态”写成“当前状态”。

连接恢复后只读检查：

```bash
cd /home/jill3/project/biyesheji
git status -sb
git remote -v
git log -5 --oneline
docker ps --filter 'name=biyesheji-' \
  --format '{{.Names}}|{{.Image}}|{{.Status}}'
./start.sh status
```

注意：

- `backups/` 是部署备份，不要删除、移动或加入 Git。
- 发现其他未提交改动时先确认来源，不要覆盖。
- 文档同步不等于应用部署；不要仅因 `git pull` 就声称容器已升级。
- 不要把服务器注册为 GitHub self-hosted Runner。

## 4. GitHub 工作流

`main` 已启用：

- 管理员也受保护。
- 严格状态检查。
- 线性历史。
- 讨论必须解决。
- 禁止强推。
- 禁止删除。

开发流程：

```bash
git switch main
git pull --ff-only origin main
git switch -c codex/inventory-insights-foundation
```

完成一个可独立验证的 Issue 后：

```bash
git status
git diff --check
git add <明确文件列表>
git commit -m "feat: add deterministic inventory insights"
git push -u origin codex/inventory-insights-foundation
gh pr create --base main --head codex/inventory-insights-foundation
```

必须等待 PR CI 和合并后 `main` CI 都完成。不要直接推送 `main`，不要跳过失败任务，不要静默刷新
评测基线来掩盖回退。

当前需要补上的仓库治理项：

- `.github/workflows/ci.yml` 已有 `evaluation`。
- README 写的是 14 项检查。
- 分支保护 API 目前只有原 13 个 required contexts。
- 在评测稳定后，把 `evaluation` 加入必需检查，并验证仍能正常合并 PR。

## 5. 当前 AI 代码入口

### 5.1 消费者对话

| 文件 | 作用 |
| --- | --- |
| `order-service/.../controller/AiController.java` | `/api/order/ai/chat` SSE 入口 |
| `order-service/.../service/impl/AiServiceImpl.java` | Prompt、历史、知识、模型流式调用、内容过滤和费用记录 |
| `biyesheji-frontend/src/views/AiAssistant.vue` | 消费者 AI 页面 |

Alpha 3 已把商品检索从 `AiServiceImpl` 抽离。不要把检索逻辑重新复制回去。

### 5.2 商品检索

| 文件 | 作用 |
| --- | --- |
| `AiRetrievalService.java` | 检索接口 |
| `AiRetrievalServiceImpl.java` | 向量索引、fallback、可售过滤和索引指纹 |
| `AiRetrievalResult.java` | 原始召回与可售候选 |
| `MerchantAiEvaluationController.java` | 默认关闭的只读评测端点 |
| `docs/AI评测接口.md` | 端点安全边界 |
| `evals/` | 确定性评测 Core、数据、配置、基线和测试 |

消费者对话当前使用：

```java
AiRetrievalResult retrieval = aiRetrievalService.retrieve(query, 10);
List<Product> candidates = retrieval.eligibleItems()...
```

新的客服意图路由不能破坏这条导购链路。

### 5.3 商家 AI 配置

| 文件 | 作用 |
| --- | --- |
| `MerchantAiSettingController.java` | 模型和预算设置 |
| `MerchantAiKnowledgeController.java` | FAQ/配送/售后/店铺知识 |
| `MerchantAiIndexTaskController.java` | 商品索引任务 |
| `AiUsageService.java` | 请求成本、指标和预算 |
| `MerchantAiSetting.vue` | 商家 AI 设置页面 |

`t_ai_request_log` 目前没有 `purpose`，新的库存解释不能继续与消费者对话混在同一用途里。

## 6. 当前库存与订单代码入口

### 6.1 商品和库存

| 文件 | 作用 |
| --- | --- |
| `product-service/.../controller/MerchantInventoryController.java` | 库存列表和低库存汇总 |
| `product-service/.../mapper/InventoryMapper.java` | 当前库存分页 SQL |
| `product-service/.../service/impl/InventoryServiceImpl.java` | 当前仅转发列表/汇总 |
| `product-service/.../service/impl/ProductServiceImpl.java` | SKU 创建、人工调整和库存流水 |
| `biyesheji-frontend/src/views/MerchantInventory.vue` | 库存页 |
| `t_stock` | `total/locked/available/version` |
| `t_stock_ledger` | 当前只有初始化和人工调整记录 |

### 6.2 订单与库存扣减

| 文件 | 作用 |
| --- | --- |
| `OrderServiceImpl.java` | 下单、收款、接单、发货、取消、超时和完成 |
| `StockServiceImpl.java` | MySQL 乐观锁和 Redis Lua 预留/释放/确认 |
| `StockLua.java` | Redis 原子库存脚本 |
| `t_order` | 状态、支付方式、`pay_time`、履约时间 |
| `t_order_item` | SKU、数量、价格和小计 |

关键缺口：

`StockServiceImpl` 修改订单库存时没有写 `t_stock_ledger`。因此不能直接声称现有流水已经能解释所有
库存变化。实施计划把补齐订单预留、释放和确认流水列为第一阶段。

## 7. 已确认业务口径

不要在开发中自行换一套口径：

- 确认销售以 `pay_time IS NOT NULL` 为基准。
- COD 发货但尚未确认收款可计入履约需求，不计入确认销售。
- SKU 销量从 `t_order_item.quantity` 聚合，不以 `t_product.sales` 作为唯一事实。
- 近 7/30/90 天使用固定窗口。
- `daysOfCover = available / (confirmedQty30d / 30)`。
- 30 天销量为 0 时 `daysOfCover` 返回 `null`，同时展示无销量原因。
- 风险优先级：缺货 → 低库存 → 滞销 → 慢销 → 超储 → 健康。
- 退款数量当前不能可靠回冲 SKU 净销量，页面必须披露这一限制。
- 缺少采购成本和采购批次时，不计算利润、资金占用、精确库龄或具体降价底线。

详细公式和默认阈值见实施计划第 6 节。

## 8. 推荐第一批开发任务

不要一次实现整份计划。第一个 PR 只做 `INV-01`。

### 8.1 INV-01：库存流水完整性

目标：

订单预留、释放和确认扣减都有幂等、事务化、可追溯的库存流水。

建议步骤：

1. 从最新 `main` 新建 `codex/inventory-ledger-integrity`。
2. 增加 Flyway 迁移：
   - `event_key`
   - `before_total/after_total`
   - `before_locked/after_locked`
3. 扩展 `StockLedger` 实体，并在 `order-service` 增加对应 Mapper。
4. 把 `OrderServiceImpl.submit` 的 `orderNo` 生成移动到库存预留之前。
5. 从订单调用方传入 `orderNo`，形成稳定 `event_key`。
6. 让 `StockServiceImpl` 在数据库库存更新成功后写流水。
7. 覆盖预留、释放、确认、重试、并发、提交失败补偿和事务回滚测试。
8. 更新 SKU 回滚手册和质量验收记录。

必须验证：

- 预留：`available` 减少、`locked` 增加、`total` 不变。
- 释放：`available` 增加、`locked` 减少、`total` 不变。
- 确认：`total` 和 `locked` 减少、`available` 不变。
- 重复事件不产生第二条流水或第二次库存变化。
- 流水失败时同一事务中的库存变化回滚。

不要在 INV-01 同时开发页面或调用大模型。

### 8.2 INV-02：确定性库存洞察

只有 INV-01 合并后再开始：

- `InventoryInsightMapper`
- 指标 DTO/VO
- 风险规则
- 列表、汇总和证据 API
- Testcontainers 数据集
- 权限和隐私测试

第一版建议位于 `order-service` 的 `/api/merchant/ai/inventory-insights`，原因已经记录在实施计划。

## 9. 测试和验证命令

后端：

```bash
./mvnw clean verify
```

针对订单服务：

```bash
./mvnw -pl order-service -am test
```

前端：

```bash
cd biyesheji-frontend
npm ci
npm run test:unit
npm run build
npm audit --audit-level=high
```

确定性评测：

```bash
python -m unittest discover -s evals/tests -v
python -m evals.eval42 --config evals/config/phonemall.mock.json
```

配置：

```bash
docker compose -f docker/docker-compose.infrastructure.yml config -q
docker compose -f docker/docker-compose.app.yml config -q
```

不要因本机缺少依赖就删除测试。优先在服务器或 GitHub 托管 Runner 中验证。

## 10. 数据库和回滚约束

- Flyway 只向前迁移。
- 新增表、可空列、带默认值列可以直接发布。
- 不要在同一版本删除或重命名旧字段。
- 应用回滚不会降级数据库。
- 新字段必须允许旧应用忽略。
- 发布前执行加密备份并校验 SHA-256。
- 任何盘点或库存迁移都必须有真实回滚/恢复演练。

制定交接时最高迁移版本为 V18。开始开发前再次检查：

```bash
ls biyesheji-common/src/main/resources/db/migration
```

不要机械使用 V19；如果远端已有新迁移，选择下一个未占用版本。

## 11. 安全红线

- 不读取或输出其他用户订单。
- 不把消费者隐私送入 Embedding。
- 不记录完整模型 Prompt、Token、密钥或订单隐私。
- 不信任客户端传入的销量、库存、金额或风险等级。
- 不允许模型直接执行写操作。
- 不使用 SKU、订单号、用户 ID 作为 Prometheus 标签。
- 不提交 `.env`、备份、评测 Token、AI Key 或数据库密码。
- 不为了让 CI 通过扩大 Trivy/Gitleaks 例外。
- 不把待人工审核的评测标签写成 Gold Set。

## 12. 服务器升级和发布

功能 PR 合并且 `main` CI 通过后，才准备服务器升级。

建议新预发布版本为后续可用的下一个 Alpha 标签，不要覆盖 `v0.8.0-alpha.3`。发布步骤必须以仓库
当前脚本和最新发布说明为准：

```bash
cd /home/jill3/project/biyesheji
git status -sb
git pull --ff-only origin main
./start.sh backup
./start.sh upgrade <new-tag>
./start.sh status
```

升级后至少验证：

- 四个应用容器健康。
- Flyway 版本正确。
- 商品、用户、订单数量未异常变化。
- 库存不变量成立。
- 原消费者导购可用。
- 新库存洞察结果与测试夹具一致。
- Prometheus targets 全部 `UP`。
- 日志无新的 ERROR/FATAL。
- 必要时实际回滚上一应用版本并恢复。

如果服务器 SSH 仍不可达，报告阻塞和已经完成的 GitHub 工作，不要声称已部署。

## 13. 交接完成状态检查表

新窗口开始工作前应确认：

- [ ] 已阅读本交接文档和实施计划。
- [ ] 已读取最新 `main`，未基于旧的 `5fed019` 直接开发。
- [ ] 已确认是否存在其他 agent/用户分支或未提交改动。
- [ ] 已确认服务器 SSH 和当前运行镜像。
- [ ] 已保留 `backups/`。
- [ ] 已创建独立 `codex/` 分支。
- [ ] 第一个 PR 范围仅为 `INV-01`。
- [ ] 已先写库存不变量和幂等测试。
- [ ] 未引入自动改价或自动库存写入。
- [ ] 已向用户提供简短、真实的阶段进度。

## 14. 相关文档

- [`AI经营与库存助手实施计划.md`](AI经营与库存助手实施计划.md)
- [`商家自托管商城开发计划.md`](商家自托管商城开发计划.md)
- [`AI评测接口.md`](AI评测接口.md)
- [`SKU迁移与回滚手册.md`](SKU迁移与回滚手册.md)
- [`质量与安全验收记录.md`](质量与安全验收记录.md)
- [`生产运维验收记录.md`](生产运维验收记录.md)
- [`发布说明与已知限制.md`](发布说明与已知限制.md)
- [`../evals/README.md`](../evals/README.md)

## 15. 给新窗口的首条执行指令

可将下面内容作为新窗口的第一条任务：

> 阅读 `docs/AI经营与库存助手开发交接.md` 和
> `docs/AI经营与库存助手实施计划.md`。先核对 GitHub 最新 `main`、CI、Release 和
> `jill3@192.168.31.39:/home/jill3/project/biyesheji` 的实际状态，保留服务器 `backups/`
> 与任何现有改动。随后只执行 `INV-01`：用向前兼容的 Flyway 迁移和自动化测试补齐订单库存
> 预留、释放、确认扣减流水及幂等事件键。通过 PR、全部 CI 和合并后主干 CI 后再同步服务器；
> 不要同时开发库存洞察页面或 AI 解释。
