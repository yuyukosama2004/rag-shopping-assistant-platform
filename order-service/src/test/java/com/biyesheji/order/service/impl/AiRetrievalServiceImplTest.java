package com.biyesheji.order.service.impl;

import com.biyesheji.entity.Product;
import com.biyesheji.entity.ProductSku;
import com.biyesheji.order.mapper.ProductMapper;
import com.biyesheji.order.mapper.ProductSkuMapper;
import com.biyesheji.order.mapper.StockMapper;
import com.biyesheji.order.service.AiRetrievalResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiRetrievalServiceImplTest {

    @Test
    void fallbackIsDeterministicAndSeparatesEligibleProducts() {
        ProductMapper products = mock(ProductMapper.class);
        ProductSkuMapper skus = mock(ProductSkuMapper.class);
        StockMapper stocks = mock(StockMapper.class);
        Product first = product(2L, "Second");
        Product second = product(1L, "First");
        ProductSku activeSku = new ProductSku();
        activeSku.setId(11L);
        activeSku.setProductId(1L);
        activeSku.setStatus(1);

        when(products.selectList(any())).thenReturn(List.of(first, second));
        when(products.selectById(1L)).thenReturn(second);
        when(products.selectById(2L)).thenReturn(first);
        when(skus.selectList(any())).thenReturn(List.of(activeSku), List.of());
        when(stocks.selectCount(any())).thenReturn(1L);

        AiRetrievalServiceImpl service = new AiRetrievalServiceImpl(
                products,
                skus,
                stocks,
                Runnable::run
        );
        service.initializeIndex();

        AiRetrievalResult result = service.retrieve("camera phone", 10);

        assertEquals("fallback_all", result.retrievalMode());
        assertFalse(result.indexReady());
        assertEquals(List.of(1L, 2L), result.retrievedItems().stream()
                .map(item -> item.product().getId())
                .toList());
        assertEquals(List.of(1, 2), result.retrievedItems().stream()
                .map(item -> item.rank())
                .toList());
        assertEquals(List.of(1L), result.eligibleItems().stream()
                .map(item -> item.product().getId())
                .toList());
    }

    private Product product(Long id, String name) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setBrand("Test");
        product.setPrice(BigDecimal.valueOf(1000));
        product.setStatus(1);
        return product;
    }
}
