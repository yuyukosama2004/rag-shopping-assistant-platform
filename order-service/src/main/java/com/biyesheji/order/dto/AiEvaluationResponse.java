package com.biyesheji.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

public record AiEvaluationResponse(
        @JsonProperty("retrieved_products") List<ProductResult> retrievedProducts,
        @JsonProperty("eligible_products") List<ProductResult> eligibleProducts,
        @JsonProperty("recommended_ids") List<Long> recommendedIds,
        String answer,
        Usage usage,
        Versions versions
) {
    public record ProductResult(
            Long id,
            Double score,
            int rank,
            String brand,
            BigDecimal price,
            boolean sellable
    ) {
    }

    public record Usage(
            @JsonProperty("retrieval_latency_ms") long retrievalLatencyMs,
            @JsonProperty("first_token_latency_ms") Long firstTokenLatencyMs,
            @JsonProperty("total_latency_ms") long totalLatencyMs,
            @JsonProperty("input_tokens") Long inputTokens,
            @JsonProperty("output_tokens") Long outputTokens,
            @JsonProperty("token_count_kind") String tokenCountKind
    ) {
    }

    public record Versions(
            String model,
            @JsonProperty("embedding_model") String embeddingModel,
            @JsonProperty("prompt_hash") String promptHash,
            @JsonProperty("setting_hash") String settingHash,
            @JsonProperty("index_fingerprint") String indexFingerprint,
            @JsonProperty("retrieval_mode") String retrievalMode,
            @JsonProperty("index_ready") boolean indexReady
    ) {
    }
}
