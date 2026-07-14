package com.biyesheji.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.biyesheji.entity.Product;
import com.biyesheji.exception.BizException;
import com.biyesheji.product.mapper.ProductMapper;
import com.biyesheji.product.mapper.ProductSkuMapper;
import com.biyesheji.product.mapper.StockLedgerMapper;
import com.biyesheji.product.mapper.StockMapper;
import com.biyesheji.utils.RedisUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductServiceImplTest {

    private final ProductMapper productMapper = mock(ProductMapper.class);
    private final ProductSkuMapper productSkuMapper = mock(ProductSkuMapper.class);
    private final ProductServiceImpl service = new ProductServiceImpl(productMapper, mock(RedisUtil.class), productSkuMapper,
            mock(StockMapper.class), mock(StockLedgerMapper.class));

    @Test
    void rejectsPublishingProductWithoutActiveSku() {
        when(productMapper.selectById(1L)).thenReturn(new Product());
        when(productSkuMapper.selectCount(any(Wrapper.class))).thenReturn(0L);

        assertThrows(BizException.class, () -> service.updateStatus(1L, 1));
    }

    @Test
    void allowsPublishingProductWithActiveSku() {
        when(productMapper.selectById(1L)).thenReturn(new Product());
        when(productSkuMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        assertDoesNotThrow(() -> service.updateStatus(1L, 1));
    }
}
