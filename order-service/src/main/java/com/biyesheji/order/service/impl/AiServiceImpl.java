package com.biyesheji.order.service.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.biyesheji.entity.AiConversation;
import com.biyesheji.entity.AiSetting;
import com.biyesheji.entity.Product;
import com.biyesheji.entity.ProductSku;
import com.biyesheji.order.mapper.AiConversationMapper;
import com.biyesheji.order.mapper.ProductMapper;
import com.biyesheji.order.mapper.ProductSkuMapper;
import com.biyesheji.order.mapper.StockMapper;
import com.biyesheji.order.service.AiService;
import com.biyesheji.order.service.AiSettingService;
import com.biyesheji.utils.RedisUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class AiServiceImpl implements AiService {

    private final ProductMapper productMapper;
    private final AiConversationMapper aiConversationMapper;
    private final RedisUtil redisUtil;
    private final Executor aiExecutor;
    private final ProductSkuMapper productSkuMapper;
    private final StockMapper stockMapper;
    private final AiSettingService aiSettingService;

    // ====== DeepSeek 配置 ======
    @Value("${deepseek.api-key:}")
    private String apiKey;

    @Value("${deepseek.model:deepseek-v4-flash}")
    private String model;

    @Value("${deepseek.base-url:https://api.deepseek.com/v1}")
    private String baseUrl;

    @Value("${ai.system-prompt:}")
    private String systemPrompt;

    // ====== OpenRouter Embedding 配置 ======
    @Value("${openrouter.api-key:}")
    private String openRouterKey;

    @Value("${openrouter.embedding-model:qwen/qwen3-embedding-4b}")
    private String embeddingModel;

    // ====== 产品向量缓存 ======
    private final Map<Long, float[]> productEmbeddings = new ConcurrentHashMap<>();
    private final Map<Long, Product> productCache = new ConcurrentHashMap<>();
    private volatile boolean embeddingsReady = false;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public AiServiceImpl(ProductMapper productMapper, AiConversationMapper aiConversationMapper,
                         RedisUtil redisUtil, @Qualifier("aiExecutor") Executor aiExecutor,
                         ProductSkuMapper productSkuMapper, StockMapper stockMapper,
                         AiSettingService aiSettingService) {
        this.productMapper = productMapper;
        this.aiConversationMapper = aiConversationMapper;
        this.redisUtil = redisUtil;
        this.aiExecutor = aiExecutor;
        this.productSkuMapper = productSkuMapper;
        this.stockMapper = stockMapper;
        this.aiSettingService = aiSettingService;
    }

    // ================================================================
    //  启动时：为所有商品生成向量
    // ================================================================
    @PostConstruct
    void initEmbeddings() {
        aiExecutor.execute(() -> {
            try {
                List<Product> all = productMapper.selectList(
                        new LambdaQueryWrapper<Product>().eq(Product::getStatus, 1));
                if (all.isEmpty()) {
                    log.warn("没有商品数据，跳过向量初始化");
                    return;
                }
                // 构建富文本描述
                List<String> texts = new ArrayList<>();
                for (Product p : all) {
                    productCache.put(p.getId(), p);
                    texts.add(buildProductText(p));
                }
                if (!StringUtils.hasText(openRouterKey)) {
                    log.warn("未配置 OPENROUTER_API_KEY，RAG向量检索已禁用");
                    return;
                }
                // 批量调用 Embedding API
                float[][] vectors = batchEmbed(texts);
                for (int i = 0; i < all.size(); i++) {
                    productEmbeddings.put(all.get(i).getId(), vectors[i]);
                }
                embeddingsReady = true;
                log.info("RAG 向量初始化完成，共 {} 款商品", all.size());
            } catch (Exception e) {
                log.error("向量初始化失败，回退到全量模式", e);
            }
        });
    }

    private String buildProductText(Product p) {
        StringBuilder sb = new StringBuilder();
        sb.append(p.getName()).append(" ").append(p.getBrand()).append(" ");
        if (p.getCategory() != null) sb.append(p.getCategory()).append(" ");
        sb.append("¥").append(p.getPrice()).append(" ");
        if (p.getDescription() != null && !p.getDescription().isBlank()) {
            sb.append(p.getDescription()).append(" ");
        }
        if (p.getSpecJson() != null && !p.getSpecJson().isBlank()) {
            String spec = p.getSpecJson();
            try {
                JSONObject specObj = JSONUtil.parseObj(spec);
                StringBuilder specStr = new StringBuilder();
                for (String key : specObj.keySet()) {
                    specStr.append(key).append(":").append(specObj.getStr(key)).append(" ");
                }
                sb.append(specStr);
            } catch (Exception e) {
                sb.append(spec);
            }
        }
        return sb.toString().trim();
    }

    // ================================================================
    //  商品描述自动丰富（DeepSeek 批处理）
    // ================================================================
    private static final String ENRICH_PROMPT =
            "你是手机评测师。请根据真实市场定位+以下硬件参数，" +
            "为这款手机写一段60-80字卖点描述。\n" +
            "格式: [目标用户群体] [2-3个核心卖点] [适用场景]\n" +
            "要求: 结合你对这款机型的真实了解来写，禁止虚构不存在的功能。" +
            "若参数与你了解的信息有冲突，以参数为准。\n\n" +
            "示例:\n" +
            "适合学生党和轻度游戏玩家。骁龙7+Gen3性能均衡功耗低，" +
            "120Hz高刷屏游戏画面流畅，5000mAh大电池续航一整天无压力，" +
            "67W快充回血迅速，千元档性价比标杆。";

    @Override
    public Map<String, Object> enrichProducts() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> success = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        long start = System.currentTimeMillis();

        if (!StringUtils.hasText(apiKey) || !StringUtils.hasText(openRouterKey)) {
            result.put("status", "error");
            result.put("message", "AI服务未配置");
            return result;
        }

        List<Product> all = productMapper.selectList(
                new LambdaQueryWrapper<Product>().eq(Product::getStatus, 1));

        if (all.isEmpty()) {
            result.put("status", "error");
            result.put("message", "没有可处理的商品");
            return result;
        }

        for (Product p : all) {
            try {
                String params = p.getName() + " | " + p.getBrand() + " | ¥" + p.getPrice() + " | " + p.getSpecJson();
                String desc = generateDescription(params);
                if (desc != null && !desc.isBlank()) {
                    p.setDescription(desc.trim());
                    productMapper.updateById(p);
                    productCache.put(p.getId(), p);
                    success.add(p.getName());
                    log.info("描述已更新: {}", p.getName());
                } else {
                    failed.add(p.getName());
                }
                // 避免限流，每次间隔500ms
                Thread.sleep(500);
            } catch (Exception e) {
                log.error("处理失败: {}", p.getName(), e);
                failed.add(p.getName());
            }
        }

        // 重建向量缓存
        if (!success.isEmpty()) {
            try {
                embeddingsReady = false;
                List<String> texts = new ArrayList<>();
                for (Product p : all) {
                    texts.add(buildProductText(p));
                }
                float[][] vectors = batchEmbed(texts);
                productEmbeddings.clear();
                for (int i = 0; i < all.size(); i++) {
                    productEmbeddings.put(all.get(i).getId(), vectors[i]);
                }
                embeddingsReady = true;
                log.info("向量缓存已重建，共 {} 款", all.size());
            } catch (Exception e) {
                log.error("向量重建失败", e);
            }
            redisUtil.deleteByPattern("product:*");
        }

        result.put("status", "ok");
        result.put("success", success.size());
        result.put("failed", failed.size());
        result.put("successList", success);
        result.put("failedList", failed);
        result.put("durationMs", System.currentTimeMillis() - start);
        return result;
    }

    private String generateDescription(String params) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("stream", false);
            body.put("messages", List.of(
                    Map.of("role", "system", "content", ENRICH_PROMPT),
                    Map.of("role", "user", "content", params)
            ));
            body.put("max_tokens", 300);

            String json = JSONUtil.toJsonStr(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.error("DeepSeek 返回 {}: {}", response.statusCode(), response.body());
                return null;
            }
            return JSONUtil.parseObj(response.body())
                    .getByPath("choices[0].message.content", String.class);
        } catch (Exception e) {
            log.error("生成描述失败", e);
            return null;
        }
    }

    // ================================================================
    //  Embedding API 调用
    // ================================================================
    private float[][] batchEmbed(List<String> texts) throws Exception {
        if (!StringUtils.hasText(openRouterKey)) {
            throw new IllegalStateException("OPENROUTER_API_KEY 未配置");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", embeddingModel);
        body.put("input", texts);
        body.put("encoding_format", "float");

        String json = JSONUtil.toJsonStr(body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://openrouter.ai/api/v1/embeddings"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + openRouterKey)
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Embedding API 返回 " + response.statusCode() + ": " + response.body());
        }

        JSONObject resp = JSONUtil.parseObj(response.body());
        int n = resp.getJSONArray("data").size();
        float[][] result = new float[n][];
        for (int i = 0; i < n; i++) {
            JSONObject item = resp.getJSONArray("data").getJSONObject(i);
            result[item.getInt("index")] = toFloatArray(item.getJSONArray("embedding"));
        }
        return result;
    }

    private float[] embedText(String text) {
        try {
            float[][] batch = batchEmbed(List.of(text));
            return batch[0];
        } catch (Exception e) {
            log.error("Embedding 调用失败", e);
            return null;
        }
    }

    private float[] toFloatArray(cn.hutool.json.JSONArray arr) {
        float[] f = new float[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            f[i] = arr.getFloat(i);
        }
        return f;
    }

    // ================================================================
    //  余弦相似度 + RAG 检索
    // ================================================================
    private double cosineSimilarity(float[] a, float[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private List<Product> ragSearch(String query, int topK) {
        if (!embeddingsReady) {
            // 向量未就绪，回退到全量
            return new ArrayList<>(productCache.values());
        }

        float[] queryVec = embedText(query);
        if (queryVec == null) {
            return new ArrayList<>(productCache.values());
        }

        // 计算所有产品与查询的余弦相似度
        record ScoredProduct(Product product, double score) {}
        List<ScoredProduct> scored = new ArrayList<>();

        for (Map.Entry<Long, float[]> entry : productEmbeddings.entrySet()) {
            Product p = productCache.get(entry.getKey());
            if (p != null) {
                double score = cosineSimilarity(queryVec, entry.getValue());
                scored.add(new ScoredProduct(p, score));
            }
        }

        scored.sort((a, b) -> Double.compare(b.score, a.score));

        List<Product> result = new ArrayList<>();
        for (int i = 0; i < Math.min(topK, scored.size()); i++) {
            result.add(scored.get(i).product);
        }
        return result;
    }

    // ================================================================
    //  AI 对话（SSE 流式）
    // ================================================================
    @Override
    public SseEmitter chat(Long userId, String query) {
        SseEmitter emitter = new SseEmitter(180_000L);

        if (!StringUtils.hasText(apiKey)) {
            try {
                emitter.send(SseEmitter.event().data("AI导购尚未配置，请联系管理员。"));
            } catch (IOException ignored) {
            }
            emitter.complete();
            return emitter;
        }
        AiSetting setting = aiSettingService.requireChatAllowed(userId);

        aiExecutor.execute(() -> {
            try {
                // RAG 检索：向量语义匹配 Top-10
                List<Product> candidates = ragSearch(query, 10).stream()
                        .filter(this::isCurrentlySellable)
                        .toList();
                String prompt = buildPrompt(query, candidates);

                Map<String, Object> body = new LinkedHashMap<>();
                body.put("model", setting.getModel());
                body.put("stream", true);
                body.put("temperature", setting.getTemperature());
                body.put("max_tokens", setting.getMaxOutputTokens());

                List<Map<String, String>> messages = new ArrayList<>();
                String configuredPrompt = StringUtils.hasText(setting.getSystemPrompt())
                        ? setting.getSystemPrompt()
                        : (StringUtils.hasText(systemPrompt) ? systemPrompt : "你是本店的 AI 导购，请诚实回答商品选购问题。");
                messages.add(Map.of("role", "system", "content", configuredPrompt));

                List<AiConversation> history = userId != null ? aiConversationMapper.selectList(
                        new LambdaQueryWrapper<AiConversation>()
                                .eq(AiConversation::getUserId, userId)
                                .orderByDesc(AiConversation::getId)
                                .last("LIMIT 10")) : List.of();
                Collections.reverse(history);
                for (AiConversation h : history) {
                    messages.add(Map.of("role", h.getRole(), "content", h.getContent()));
                }
                saveMessage(userId, "user", query, null);
                if (StringUtils.hasText(setting.getDisclaimer())) {
                    prompt += "\n\n回答末尾请原样附上提示：" + setting.getDisclaimer();
                }
                messages.add(Map.of("role", "user", "content", prompt));

                body.put("messages", messages);

                String json = JSONUtil.toJsonStr(body);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/chat/completions"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + apiKey)
                        .timeout(Duration.ofSeconds(120))
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                HttpResponse<java.io.InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

                if (response.statusCode() != 200) {
                    String errBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                    log.error("AI API 返回非200: status={}, body={}", response.statusCode(), errBody);
                    emitter.send(SseEmitter.event().data("抱歉，AI助手暂时无法响应（" + response.statusCode() + "）。"));
                    emitter.complete();
                    return;
                }

                StringBuilder fullReply = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data:")) {
                            String data = line.substring(5).trim();
                            if ("[DONE]".equals(data)) continue;
                            String text = extractContent(data);
                            if (text != null && !text.isEmpty()) {
                                fullReply.append(text);
                                emitter.send(SseEmitter.event().data(text));
                            }
                        }
                    }
                }
                if (fullReply.length() > 0) {
                    saveMessage(userId, "assistant", fullReply.toString(), null);
                }
                emitter.complete();
            } catch (Exception e) {
                log.error("AI 服务调用失败", e);
                try {
                    emitter.send(SseEmitter.event().data("抱歉，AI助手暂时无法响应，请重试。"));
                    emitter.complete();
                } catch (IOException ignored) {}
                saveMessage(userId, "assistant", "抱歉，AI助手暂时无法响应，请重试。", null);
            }
        });

        return emitter;
    }

    boolean isCurrentlySellable(Product cached) {
        Product current = productMapper.selectById(cached.getId());
        if (current == null || current.getStatus() == null || current.getStatus() != 1) return false;
        List<Long> activeSkuIds = productSkuMapper.selectList(new LambdaQueryWrapper<ProductSku>()
                        .eq(ProductSku::getProductId, cached.getId())
                        .eq(ProductSku::getStatus, 1)).stream()
                .map(ProductSku::getId)
                .toList();
        return !activeSkuIds.isEmpty() && stockMapper.selectCount(new LambdaQueryWrapper<com.biyesheji.entity.Stock>()
                .in(com.biyesheji.entity.Stock::getSkuId, activeSkuIds)
                .gt(com.biyesheji.entity.Stock::getAvailable, 0)) > 0;
    }

    @Override
    public AiConversation saveMessage(Long userId, String role, String content, String recommendations) {
        AiConversation conv = new AiConversation();
        conv.setUserId(userId);
        conv.setRole(role);
        conv.setContent(content);
        conv.setRecommendations(recommendations);
        aiConversationMapper.insert(conv);
        return conv;
    }

    // ================================================================
    //  工具方法
    // ================================================================
    private String extractContent(String data) {
        try {
            JSONObject obj = JSONUtil.parseObj(data);
            // 只取 content（最终结果），跳过 reasoning_content（思考过程）
            String content = obj.getByPath("choices[0].delta.content", String.class);
            return content != null ? content : "";
        } catch (Exception e) { return ""; }
    }

    String buildPrompt(String query, List<Product> candidates) {
        StringBuilder sb = new StringBuilder();
        sb.append("用户需求: ").append(query).append("\n\n可选机型:\n");
        for (Product p : candidates) {
            sb.append("- ").append(p.getName())
              .append(" | ").append(p.getBrand())
              .append(" | ¥").append(p.getPrice());
            if (p.getDescription() != null && !p.getDescription().isBlank()) {
                sb.append(" | ").append(p.getDescription().trim());
            }
            sb.append(" | 配置: ").append(p.getSpecJson())
              .append(" | 销量: ").append(p.getSales())
              .append(" | SKU: ").append(p.getId()).append("\n");
        }
        sb.append("\n只能推荐以上候选列表中的商品，不得编造商品、价格、库存、优惠或售后承诺。");
        if (candidates.isEmpty()) {
            sb.append("当前没有可售候选商品，请明确告知用户并建议联系商家，不得自行推荐列表外商品。");
        } else {
            sb.append("请从候选列表中推荐1-3款最合适的商品。");
        }
        return sb.toString();
    }
}
