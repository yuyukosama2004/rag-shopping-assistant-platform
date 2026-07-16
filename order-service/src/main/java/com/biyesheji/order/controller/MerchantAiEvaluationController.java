package com.biyesheji.order.controller;

import com.biyesheji.constant.UserRole;
import com.biyesheji.exception.BizException;
import com.biyesheji.order.dto.AiEvaluationRequest;
import com.biyesheji.order.dto.AiEvaluationResponse;
import com.biyesheji.order.service.AiRetrievalItem;
import com.biyesheji.order.service.AiRetrievalResult;
import com.biyesheji.order.service.AiRetrievalService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/merchant/ai")
public class MerchantAiEvaluationController {

    private final AiRetrievalService retrievalService;
    private final boolean evaluationEnabled;
    private final String model;
    private final String embeddingModel;

    public MerchantAiEvaluationController(
            AiRetrievalService retrievalService,
            @Value("${ai.evaluation-enabled:false}") boolean evaluationEnabled,
            @Value("${deepseek.model:deepseek-v4-flash}") String model,
            @Value("${openrouter.embedding-model:qwen/qwen3-embedding-4b}") String embeddingModel
    ) {
        this.retrievalService = retrievalService;
        this.evaluationEnabled = evaluationEnabled;
        this.model = model;
        this.embeddingModel = embeddingModel;
    }

    @PostMapping("/evaluate")
    public AiEvaluationResponse evaluate(
            @RequestHeader("X-User-Role") Integer role,
            @Valid @RequestBody AiEvaluationRequest request
    ) {
        requireMerchant(role);
        if (!evaluationEnabled) {
            throw new BizException(404, "AI evaluation endpoint is disabled");
        }

        long startedAt = System.nanoTime();
        AiRetrievalResult result = retrievalService.retrieve(request.getQuery().trim(), 10);
        long latencyMs = (System.nanoTime() - startedAt) / 1_000_000;
        Set<Long> eligibleIds = result.eligibleItems().stream()
                .map(item -> item.product().getId())
                .collect(Collectors.toSet());

        AiEvaluationResponse response = new AiEvaluationResponse(
                toResponseItems(result.retrievedItems(), eligibleIds),
                toResponseItems(result.eligibleItems(), eligibleIds),
                List.of(),
                null,
                new AiEvaluationResponse.Usage(
                        latencyMs,
                        null,
                        latencyMs,
                        null,
                        null,
                        "unavailable"
                ),
                new AiEvaluationResponse.Versions(
                        model,
                        embeddingModel,
                        null,
                        null,
                        result.indexFingerprint(),
                        result.retrievalMode(),
                        result.indexReady()
                )
        );
        return response;
    }

    private List<AiEvaluationResponse.ProductResult> toResponseItems(
            List<AiRetrievalItem> items,
            Set<Long> eligibleIds
    ) {
        return items.stream()
                .map(item -> new AiEvaluationResponse.ProductResult(
                        item.product().getId(),
                        item.score(),
                        item.rank(),
                        item.product().getBrand(),
                        item.product().getPrice(),
                        eligibleIds.contains(item.product().getId())
                ))
                .toList();
    }

    private void requireMerchant(Integer role) {
        if (!Integer.valueOf(UserRole.OWNER).equals(role)
                && !Integer.valueOf(UserRole.STAFF).equals(role)) {
            throw new BizException(403, "Only merchants may run AI evaluation");
        }
    }
}
