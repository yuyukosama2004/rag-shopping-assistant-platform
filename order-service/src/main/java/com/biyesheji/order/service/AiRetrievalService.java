package com.biyesheji.order.service;

import com.biyesheji.entity.Product;

public interface AiRetrievalService {

    AiRetrievalResult retrieve(String query, int topK);

    boolean isCurrentlySellable(Product product);

    void refreshProductIndex(Long productId, String operation);

    void rebuildIndex();
}
