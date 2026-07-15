package com.biyesheji.order.service.impl;

import com.biyesheji.entity.Product;
import com.biyesheji.entity.ProductSku;
import com.biyesheji.order.mapper.AiConversationMapper;
import com.biyesheji.order.mapper.ProductMapper;
import com.biyesheji.order.mapper.ProductSkuMapper;
import com.biyesheji.order.mapper.StockMapper;
import com.biyesheji.utils.RedisUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiServiceImplTest {
    @Test
    void requiresPublishedProductWithAvailableActiveSku() {
        ProductMapper products = mock(ProductMapper.class);
        ProductSkuMapper skus = mock(ProductSkuMapper.class);
        StockMapper stocks = mock(StockMapper.class);
        AiServiceImpl service = new AiServiceImpl(products, mock(AiConversationMapper.class), mock(RedisUtil.class), Runnable::run, skus, stocks);
        Product product = new Product(); product.setId(1L); product.setStatus(1);
        ProductSku sku = new ProductSku(); sku.setId(11L); sku.setProductId(1L); sku.setStatus(1);
        when(products.selectById(1L)).thenReturn(product);
        when(skus.selectList(any())).thenReturn(List.of(sku));
        when(stocks.selectCount(any())).thenReturn(1L, 0L);

        assertTrue(service.isCurrentlySellable(product));
        assertFalse(service.isCurrentlySellable(product));
    }

    @Test
    void emptyCandidatesForbidInventedRecommendations() {
        AiServiceImpl service = new AiServiceImpl(mock(ProductMapper.class), mock(AiConversationMapper.class), mock(RedisUtil.class), Runnable::run, mock(ProductSkuMapper.class), mock(StockMapper.class));

        String prompt = service.buildPrompt("推荐手机", List.of());

        assertTrue(prompt.contains("不得自行推荐列表外商品"));
        assertTrue(prompt.contains("联系商家"));
    }
}
