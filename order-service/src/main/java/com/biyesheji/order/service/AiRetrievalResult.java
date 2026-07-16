package com.biyesheji.order.service;

import java.util.List;

public record AiRetrievalResult(
        List<AiRetrievalItem> retrievedItems,
        List<AiRetrievalItem> eligibleItems,
        String retrievalMode,
        boolean indexReady,
        String indexFingerprint
) {
    public AiRetrievalResult {
        retrievedItems = List.copyOf(retrievedItems);
        eligibleItems = List.copyOf(eligibleItems);
    }
}
