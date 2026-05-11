package com.biyesheji.order.service.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.biyesheji.entity.AiConversation;
import com.biyesheji.entity.Product;
import com.biyesheji.order.mapper.AiConversationMapper;
import com.biyesheji.order.mapper.ProductMapper;
import com.biyesheji.order.service.AiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class AiServiceImpl implements AiService {

    private final ProductMapper productMapper;
    private final AiConversationMapper aiConversationMapper;

    @Value("${openrouter.api-key:}")
    private String apiKey;

    @Value("${openrouter.model:deepseek-v4-flash}")
    private String model;

    @Value("${openrouter.base-url:https://api.deepseek.com/v1}")
    private String baseUrl;

    public AiServiceImpl(ProductMapper productMapper, AiConversationMapper aiConversationMapper) {
        this.productMapper = productMapper;
        this.aiConversationMapper = aiConversationMapper;
    }

    @Override
    public SseEmitter chat(Long userId, String query) {
        saveMessage(userId, "user", query, null);
        SseEmitter emitter = new SseEmitter(120_000L); // 2 分钟超时

        new Thread(() -> {
            try {
                AiIntent intent = parseIntent(query);
                List<Product> candidates = searchProducts(intent);
                String prompt = buildPrompt(query, candidates);

                Map<String, Object> body = new LinkedHashMap<>();
                body.put("model", model);
                body.put("stream", true);
                body.put("messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", prompt)
                ));

                String json = JSONUtil.toJsonStr(body);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/chat/completions"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + apiKey)
                        .timeout(Duration.ofSeconds(60))
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
                HttpResponse<java.io.InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

                if (response.statusCode() != 200) {
                    String errBody = new String(response.body().readAllBytes());
                    log.error("AI API 返回非200: status={}, body={}", response.statusCode(), errBody);
                    emitter.send(SseEmitter.event().data("抱歉，AI助手暂时无法响应（" + response.statusCode() + "）。"));
                    emitter.complete();
                    return;
                }

                java.io.InputStream is = response.body();
                byte[] buf = new byte[4096];
                int n;
                StringBuilder fullReply = new StringBuilder();
                StringBuilder lineBuf = new StringBuilder();

                while ((n = is.read(buf)) != -1) {
                    String chunk = new String(buf, 0, n);
                    for (char c : chunk.toCharArray()) {
                        lineBuf.append(c);
                        if (c == '\n') {
                            String line = lineBuf.toString().trim();
                            lineBuf.setLength(0);
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
                }
                is.close();
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
        }).start();

        return emitter;
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

    private static final String SYSTEM_PROMPT =
            "你是一位拥有5年经验的手机产品评测师。基于提供的商品数据库，根据用户需求推荐最合适的手机。\n\n" +
            "规则:\n" +
            "1. 只推荐数据库中存在的机型，禁止虚构\n" +
            "2. 优先考虑用户最关心的需求维度\n" +
            "3. 每组推荐包含: 机型名称 + 核心配置 + 推荐理由\n" +
            "4. 推荐末尾附上 SKU ID (skuId) 和直达购买链接格式: /product/{id}\n" +
            "5. 回复语气专业但亲切，控制在200字以内";

    private String extractContent(String data) {
        try { return JSONUtil.parseObj(data).getByPath("choices[0].delta.content", String.class); }
        catch (Exception e) { return ""; }
    }

    private AiIntent parseIntent(String query) {
        AiIntent intent = new AiIntent();
        Pattern pricePattern = Pattern.compile("(\\d+)\\s*[-到至]\\s*(\\d+)\\s*(元|块)?");
        Matcher m = pricePattern.matcher(query);
        if (m.find()) {
            intent.minPrice = new BigDecimal(m.group(1));
            intent.maxPrice = new BigDecimal(m.group(2));
        } else {
            Pattern singlePrice = Pattern.compile("(\\d{3,5})\\s*(元|块|以内|以下|左右)?");
            Matcher sm = singlePrice.matcher(query);
            if (sm.find()) {
                BigDecimal price = new BigDecimal(sm.group(1));
                intent.maxPrice = price.add(new BigDecimal("1000"));
                intent.minPrice = price.subtract(new BigDecimal("500"));
            }
        }
        String[] brands = {"Apple","Samsung","Xiaomi","Huawei","OPPO","vivo","OnePlus","Honor","realme","Redmi","iQOO","Nubia","Motorola"};
        for (String b : brands) { if (query.toLowerCase().contains(b.toLowerCase())) { intent.brand = b; break; } }
        String lower = query.toLowerCase();
        if (lower.contains("游戏") || lower.contains("打游戏") || lower.contains("性能")) intent.gaming = true;
        if (lower.contains("拍照") || lower.contains("相机") || lower.contains("摄像")) intent.camera = true;
        if (lower.contains("续航") || lower.contains("电池")) intent.battery = true;
        if (lower.contains("轻薄") || lower.contains("轻") || lower.contains("薄")) intent.slim = true;
        if (lower.contains("屏幕") || lower.contains("显示")) intent.screen = true;
        return intent;
    }

    private List<Product> searchProducts(AiIntent intent) {
        LambdaQueryWrapper<Product> w = new LambdaQueryWrapper<>();
        w.eq(Product::getStatus, 1);
        if (intent.brand != null) w.eq(Product::getBrand, intent.brand);
        if (intent.minPrice != null) w.ge(Product::getPrice, intent.minPrice);
        if (intent.maxPrice != null) w.le(Product::getPrice, intent.maxPrice);
        w.orderByDesc(Product::getSales);
        w.last("LIMIT 15");
        return productMapper.selectList(w);
    }

    private String buildPrompt(String query, List<Product> candidates) {
        StringBuilder sb = new StringBuilder();
        sb.append("用户需求: ").append(query).append("\n\n可选机型:\n");
        for (Product p : candidates) {
            sb.append("- ").append(p.getName()).append(" | ").append(p.getBrand())
              .append(" | ¥").append(p.getPrice()).append(" | 配置: ").append(p.getSpecJson())
              .append(" | 销量: ").append(p.getSales()).append(" | SKU: ").append(p.getId()).append("\n");
        }
        sb.append("\n请基于以上数据推荐1-3款最合适的手机。");
        if (candidates.isEmpty()) sb.append("（无匹配机型，请告知用户扩大范围。）");
        return sb.toString();
    }

    static class AiIntent {
        BigDecimal minPrice; BigDecimal maxPrice; String brand;
        boolean gaming, camera, battery, slim, screen;
    }
}
