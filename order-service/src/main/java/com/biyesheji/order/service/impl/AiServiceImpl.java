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
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class AiServiceImpl implements AiService {

    private final ProductMapper productMapper;
    private final AiConversationMapper aiConversationMapper;
    private final WebClient webClient;

    @Value("${openrouter.api-key:sk-or-v1-default}")
    private String apiKey;

    @Value("${openrouter.model:anthropic/claude-haiku-3.5}")
    private String model;

    private static final String SYSTEM_PROMPT =
            "你是一位拥有5年经验的手机产品评测师。基于提供的商品数据库，根据用户需求推荐最合适的手机。\n\n" +
            "规则:\n" +
            "1. 只推荐数据库中存在的机型，禁止虚构\n" +
            "2. 优先考虑用户最关心的需求维度\n" +
            "3. 每组推荐包含: 机型名称 + 核心配置 + 推荐理由\n" +
            "4. 推荐末尾附上 SKU ID (skuId) 和直达购买链接格式: /product/{id}\n" +
            "5. 回复语气专业但亲切，控制在200字以内";

    public AiServiceImpl(ProductMapper productMapper, AiConversationMapper aiConversationMapper) {
        this.productMapper = productMapper;
        this.aiConversationMapper = aiConversationMapper;
        this.webClient = WebClient.builder()
                .baseUrl("https://openrouter.ai/api/v1")
                .build();
    }

    @Override
    public Flux<ServerSentEvent<String>> chat(Long userId, String query) {
        // 1. 保存用户消息
        saveMessage(userId, "user", query, null);

        // 2. 解析意图
        AiIntent intent = parseIntent(query);

        // 3. 查询匹配商品
        List<Product> candidates = searchProducts(intent);

        // 4. 构建 Prompt
        String prompt = buildPrompt(query, candidates);

        // 5. 调用 OpenRouter
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", model);
        requestBody.put("stream", true);
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", prompt)
        ));

        // 用于累积 AI 回复全文以便存储
        StringBuilder fullReply = new StringBuilder();

        return webClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(String.class)
                .filter(data -> data != null && !data.isEmpty() && !"[DONE]".equals(data.trim()))
                .map(raw -> {
                    // 解析 OpenAI 格式 SSE，提取纯文本
                    String text = extractContent(raw);
                    fullReply.append(text);
                    return ServerSentEvent.<String>builder()
                            .data(text)
                            .build();
                })
                .doOnTerminate(() -> {
                    if (fullReply.length() > 0) {
                        saveMessage(userId, "assistant", fullReply.toString(), null);
                    }
                })
                .onErrorResume(e -> {
                    log.error("AI 服务调用失败", e);
                    String errorMsg = "抱歉，AI助手暂时无法响应，请重试或直接浏览商品列表挑选。";
                    saveMessage(userId, "assistant", errorMsg, null);
                    return Flux.just(
                            ServerSentEvent.<String>builder()
                                    .data(errorMsg)
                                    .build(),
                            ServerSentEvent.<String>builder()
                                    .data("[DONE]")
                                    .build()
                    );
                });
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

    private AiIntent parseIntent(String query) {
        AiIntent intent = new AiIntent();

        // 提取预算
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
                if (sm.group(2) != null && sm.group(2).contains("以内") || sm.group(2) != null && sm.group(2).contains("以下")) {
                    intent.maxPrice = price;
                } else {
                    intent.maxPrice = price.add(new BigDecimal("1000"));
                    intent.minPrice = price.subtract(new BigDecimal("500"));
                }
            }
        }

        // 品牌匹配
        String[] brands = {"Apple", "Samsung", "Xiaomi", "Huawei", "OPPO", "vivo", "OnePlus",
                "Honor", "realme", "Sony", "Redmi", "iQOO", "Nothing", "Meizu", "Nubia", "ASUS", "Motorola", "ZTE"};
        for (String brand : brands) {
            if (query.toLowerCase().contains(brand.toLowerCase())) {
                intent.brand = brand;
                break;
            }
        }

        // 功能偏好
        String lower = query.toLowerCase();
        if (lower.contains("游戏") || lower.contains("打游戏") || lower.contains("性能")) intent.gaming = true;
        if (lower.contains("拍照") || lower.contains("相机") || lower.contains("摄像") || lower.contains("摄影")) intent.camera = true;
        if (lower.contains("续航") || lower.contains("电池") || lower.contains("待机")) intent.battery = true;
        if (lower.contains("轻薄") || lower.contains("轻") || lower.contains("薄")) intent.slim = true;
        if (lower.contains("屏幕") || lower.contains("显示") || lower.contains("4k")) intent.screen = true;

        return intent;
    }

    private List<Product> searchProducts(AiIntent intent) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, 1);

        if (intent.brand != null) {
            wrapper.eq(Product::getBrand, intent.brand);
        }
        if (intent.minPrice != null) {
            wrapper.ge(Product::getPrice, intent.minPrice);
        }
        if (intent.maxPrice != null) {
            wrapper.le(Product::getPrice, intent.maxPrice);
        }

        wrapper.orderByDesc(Product::getSales);
        wrapper.last("LIMIT 15");
        return productMapper.selectList(wrapper);
    }

    private String buildPrompt(String query, List<Product> candidates) {
        StringBuilder sb = new StringBuilder();
        sb.append("用户需求: ").append(query).append("\n\n");
        sb.append("可选机型:\n");
        for (Product p : candidates) {
            sb.append("- ").append(p.getName())
                    .append(" | ").append(p.getBrand())
                    .append(" | ¥").append(p.getPrice())
                    .append(" | 配置: ").append(p.getSpecJson())
                    .append(" | 销量: ").append(p.getSales())
                    .append(" | SKU: ").append(p.getId())
                    .append("\n");
        }
        sb.append("\n请基于以上数据推荐1-3款最合适的手机。");
        if (candidates.isEmpty()) {
            sb.append("（注意：当前筛选条件无匹配机型，请告知用户并建议扩大预算范围或更换品牌。）");
        }
        return sb.toString();
    }

    /**
     * 解析 SSE 行，提取 OpenAI 格式 JSON 中的文本 content
     */
    private String extractContent(String raw) {
        try {
            String jsonStr = raw;
            if (raw.startsWith("data:")) {
                jsonStr = raw.substring(5).trim();
            }
            JSONObject json = JSONUtil.parseObj(jsonStr);
            return json.getByPath("choices[0].delta.content", String.class);
        } catch (Exception e) {
            return ""; // 无法解析的帧（如 HTTP 头），忽略
        }
    }

    static class AiIntent {
        BigDecimal minPrice;
        BigDecimal maxPrice;
        String brand;
        boolean gaming;
        boolean camera;
        boolean battery;
        boolean slim;
        boolean screen;
    }
}
