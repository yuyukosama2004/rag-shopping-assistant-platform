package com.biyesheji.order.service.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.biyesheji.entity.Product;
import com.biyesheji.entity.ProductSku;
import com.biyesheji.entity.Stock;
import com.biyesheji.order.mapper.ProductMapper;
import com.biyesheji.order.mapper.ProductSkuMapper;
import com.biyesheji.order.mapper.StockMapper;
import com.biyesheji.order.service.AiRetrievalItem;
import com.biyesheji.order.service.AiRetrievalResult;
import com.biyesheji.order.service.AiRetrievalService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class AiRetrievalServiceImpl implements AiRetrievalService {

    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final StockMapper stockMapper;
    private final Executor aiExecutor;
    private final Map<Long, float[]> productEmbeddings = new ConcurrentHashMap<>();
    private final Map<Long, Product> productCache = new ConcurrentHashMap<>();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${openrouter.api-key:}")
    private String openRouterKey;

    @Value("${openrouter.embedding-model:qwen/qwen3-embedding-4b}")
    private String embeddingModel;

    private volatile boolean embeddingsReady;

    public AiRetrievalServiceImpl(
            ProductMapper productMapper,
            ProductSkuMapper productSkuMapper,
            StockMapper stockMapper,
            @Qualifier("aiExecutor") Executor aiExecutor
    ) {
        this.productMapper = productMapper;
        this.productSkuMapper = productSkuMapper;
        this.stockMapper = stockMapper;
        this.aiExecutor = aiExecutor;
    }

    @PostConstruct
    void initializeIndex() {
        aiExecutor.execute(() -> {
            try {
                rebuildIndex();
            } catch (Exception exception) {
                embeddingsReady = false;
                log.error("AI product index initialization failed; retrieval will use fallback_all", exception);
            }
        });
    }

    @Override
    public AiRetrievalResult retrieve(String query, int topK) {
        if (topK < 1) {
            throw new IllegalArgumentException("topK must be at least 1");
        }
        if (!embeddingsReady) {
            return fallbackResult("fallback_all");
        }

        float[] queryVector = embedText(query);
        if (queryVector == null) {
            return fallbackResult("fallback_all");
        }

        List<ScoredProduct> scored = new ArrayList<>();
        for (Map.Entry<Long, float[]> entry : productEmbeddings.entrySet()) {
            Product product = productCache.get(entry.getKey());
            if (product != null) {
                scored.add(new ScoredProduct(
                        product,
                        cosineSimilarity(queryVector, entry.getValue())
                ));
            }
        }
        scored.sort(
                Comparator.comparingDouble(ScoredProduct::score)
                        .reversed()
                        .thenComparing(item -> item.product().getId())
        );

        List<AiRetrievalItem> retrieved = new ArrayList<>();
        for (int index = 0; index < Math.min(topK, scored.size()); index++) {
            ScoredProduct item = scored.get(index);
            retrieved.add(new AiRetrievalItem(item.product(), item.score(), index + 1));
        }
        return buildResult(retrieved, "vector", true);
    }

    private AiRetrievalResult fallbackResult(String mode) {
        List<Product> products = productCache.values().stream()
                .sorted(Comparator.comparing(Product::getId))
                .toList();
        List<AiRetrievalItem> retrieved = new ArrayList<>();
        for (int index = 0; index < products.size(); index++) {
            retrieved.add(new AiRetrievalItem(products.get(index), null, index + 1));
        }
        return buildResult(retrieved, mode, false);
    }

    private AiRetrievalResult buildResult(
            List<AiRetrievalItem> retrieved,
            String mode,
            boolean indexReady
    ) {
        List<AiRetrievalItem> eligible = retrieved.stream()
                .filter(item -> isCurrentlySellable(item.product()))
                .toList();
        return new AiRetrievalResult(
                retrieved,
                eligible,
                mode,
                indexReady,
                indexFingerprint()
        );
    }

    @Override
    public boolean isCurrentlySellable(Product cached) {
        Product current = productMapper.selectById(cached.getId());
        if (current == null || !Integer.valueOf(1).equals(current.getStatus())) {
            return false;
        }
        List<Long> activeSkuIds = productSkuMapper.selectList(
                        new LambdaQueryWrapper<ProductSku>()
                                .eq(ProductSku::getProductId, cached.getId())
                                .eq(ProductSku::getStatus, 1)
                ).stream()
                .map(ProductSku::getId)
                .toList();
        return !activeSkuIds.isEmpty()
                && stockMapper.selectCount(
                new LambdaQueryWrapper<Stock>()
                        .in(Stock::getSkuId, activeSkuIds)
                        .gt(Stock::getAvailable, 0)
        ) > 0;
    }

    @Override
    public void refreshProductIndex(Long productId, String operation) {
        Product current = productMapper.selectById(productId);
        if ("DELETE".equals(operation)
                || current == null
                || !Integer.valueOf(1).equals(current.getStatus())) {
            productCache.remove(productId);
            productEmbeddings.remove(productId);
            return;
        }
        productCache.put(productId, current);
        if (!StringUtils.hasText(openRouterKey)) {
            embeddingsReady = false;
            throw new IllegalStateException(
                    "OPENROUTER_API_KEY is not configured; vector index cannot be updated"
            );
        }
        float[] vector = embedText(buildProductText(current));
        if (vector == null) {
            embeddingsReady = false;
            throw new IllegalStateException("Product embedding generation failed");
        }
        productEmbeddings.put(productId, vector);
        embeddingsReady = productEmbeddings.keySet().containsAll(productCache.keySet());
    }

    @Override
    public synchronized void rebuildIndex() {
        List<Product> products = productMapper.selectList(
                new LambdaQueryWrapper<Product>().eq(Product::getStatus, 1)
        );
        productCache.clear();
        productEmbeddings.clear();
        for (Product product : products) {
            productCache.put(product.getId(), product);
        }
        embeddingsReady = false;

        if (products.isEmpty()) {
            log.warn("AI product index has no published products");
            return;
        }
        if (!StringUtils.hasText(openRouterKey)) {
            log.warn("OPENROUTER_API_KEY is not configured; retrieval will use fallback_all");
            return;
        }

        try {
            float[][] vectors = batchEmbed(products.stream()
                    .map(this::buildProductText)
                    .toList());
            if (vectors.length != products.size()) {
                throw new IllegalStateException("Embedding response count does not match product count");
            }
            for (int index = 0; index < products.size(); index++) {
                if (vectors[index] == null) {
                    throw new IllegalStateException(
                            "Embedding response is missing product index " + index
                    );
                }
                productEmbeddings.put(products.get(index).getId(), vectors[index]);
            }
            embeddingsReady = true;
            log.info("AI product vector index rebuilt with {} products", products.size());
        } catch (Exception exception) {
            productEmbeddings.clear();
            embeddingsReady = false;
            throw new IllegalStateException("AI product vector index rebuild failed", exception);
        }
    }

    private float[][] batchEmbed(List<String> texts) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", embeddingModel);
        body.put("input", texts);
        body.put("encoding_format", "float");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://openrouter.ai/api/v1/embeddings"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + openRouterKey)
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(JSONUtil.toJsonStr(body)))
                .build();
        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );
        if (response.statusCode() != 200) {
            throw new IllegalStateException(
                    "Embedding API returned status " + response.statusCode()
            );
        }

        JSONObject payload = JSONUtil.parseObj(response.body());
        int size = payload.getJSONArray("data").size();
        float[][] result = new float[size][];
        for (int index = 0; index < size; index++) {
            JSONObject item = payload.getJSONArray("data").getJSONObject(index);
            result[item.getInt("index")] = toFloatArray(item.getJSONArray("embedding"));
        }
        return result;
    }

    private float[] embedText(String text) {
        try {
            return batchEmbed(List.of(text))[0];
        } catch (Exception exception) {
            log.error("Query embedding failed; retrieval will use fallback_all", exception);
            return null;
        }
    }

    private String buildProductText(Product product) {
        StringBuilder text = new StringBuilder();
        text.append(product.getName()).append(" ").append(product.getBrand()).append(" ");
        if (product.getCategory() != null) {
            text.append(product.getCategory()).append(" ");
        }
        text.append("¥").append(product.getPrice()).append(" ");
        if (StringUtils.hasText(product.getDescription())) {
            text.append(product.getDescription()).append(" ");
        }
        if (StringUtils.hasText(product.getSpecJson())) {
            try {
                JSONObject specs = JSONUtil.parseObj(product.getSpecJson());
                for (String key : specs.keySet()) {
                    text.append(key).append(":").append(specs.getStr(key)).append(" ");
                }
            } catch (Exception ignored) {
                text.append(product.getSpecJson());
            }
        }
        return text.toString().trim();
    }

    private double cosineSimilarity(float[] left, float[] right) {
        double dot = 0;
        double leftNorm = 0;
        double rightNorm = 0;
        for (int index = 0; index < left.length; index++) {
            dot += (double) left[index] * right[index];
            leftNorm += (double) left[index] * left[index];
            rightNorm += (double) right[index] * right[index];
        }
        if (leftNorm == 0 || rightNorm == 0) {
            return 0;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private float[] toFloatArray(cn.hutool.json.JSONArray values) {
        float[] result = new float[values.size()];
        for (int index = 0; index < values.size(); index++) {
            result[index] = values.getFloat(index);
        }
        return result;
    }

    private String indexFingerprint() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            productCache.values().stream()
                    .sorted(Comparator.comparing(Product::getId))
                    .forEach(product -> {
                        float[] vector = productEmbeddings.get(product.getId());
                        String row = product.getId()
                                + "|" + buildProductText(product)
                                + "|" + (vector == null ? "none" : Arrays.hashCode(vector))
                                + "\n";
                        digest.update(row.getBytes(StandardCharsets.UTF_8));
                    });
            return "sha256:" + HexFormat.of().formatHex(digest.digest());
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to fingerprint AI product index", exception);
        }
    }

    private record ScoredProduct(Product product, double score) {
    }
}
