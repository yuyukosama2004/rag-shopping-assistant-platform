package com.biyesheji.order.service;

import com.biyesheji.entity.Product;

public record AiRetrievalItem(Product product, Double score, int rank) {
}
