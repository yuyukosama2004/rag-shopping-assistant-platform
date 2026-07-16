# AI 检索评测接口

PhoneMall 提供一个面向商家、默认关闭的只读检索端点，供 Eval42 等受控评测工具使用：

```text
POST /api/merchant/ai/evaluate
```

## 安全边界

- 由现有网关认证，只有店主或店员可以访问。
- `AI_EVAL_ENABLED=false` 时返回 404；生产环境应保持默认关闭。
- 端点不读取用户聊天历史，不写入 `AiConversation`，不修改商品、库存或订单。
- 端点不调用生成模型，不占用用户 AI 日额度；`generate_answer=true` 会被拒绝。
- 返回模型和索引版本信息，但不返回 API Key、系统 Prompt 或内部堆栈。

## 启用

仅在受保护的评测环境中设置：

```dotenv
AI_EVAL_ENABLED=true
```

## 请求

```json
{
  "query": "预算 4000 元，重视拍照和续航，不要苹果",
  "generate_answer": false
}
```

请求需要正常的商家 Bearer Token。网关会校验 Token 并向订单服务注入可信的
`X-User-Role`，客户端自行伪造该 Header 不会绕过认证。

## 响应语义

- `retrieved_products`：过滤前的原始检索排名。
- `eligible_products`：使用生产聊天相同的上架、有效 SKU 和可用库存过滤后的候选。
- `score`：向量相似度；fallback 模式下为 `null`。
- `rank`：原始检索排名。可售过滤不会重排，因此排名可能存在空档。
- `retrieval_mode=vector`：向量索引和查询 Embedding 正常。
- `retrieval_mode=fallback_all`：索引未就绪或查询 Embedding 失败，返回确定性排序的缓存商品。
- `index_ready`：本次结果是否来自完整向量索引。

该端点直接返回评测对象，不套用通用 `R` 响应外壳，以便外部 Adapter 稳定映射字段。
