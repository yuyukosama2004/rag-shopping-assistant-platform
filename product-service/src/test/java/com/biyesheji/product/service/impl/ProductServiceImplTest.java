package com.biyesheji.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.biyesheji.dto.StockAdjustDTO;
import com.biyesheji.entity.Product;
import com.biyesheji.entity.ProductSku;
import com.biyesheji.entity.Stock;
import com.biyesheji.entity.StockLedger;
import com.biyesheji.dto.MerchantSkuUpdateDTO;
import com.biyesheji.exception.BizException;
import com.biyesheji.product.mapper.ProductMapper;
import com.biyesheji.product.mapper.ProductSkuMapper;
import com.biyesheji.product.mapper.StockLedgerMapper;
import com.biyesheji.product.mapper.StockMapper;
import com.biyesheji.product.service.AiIndexTaskPublisher;
import com.biyesheji.utils.RedisUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.mockito.ArgumentCaptor;
import org.apache.ibatis.builder.MapperBuilderAssistant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductServiceImplTest {

    @BeforeAll
    static void initializeStockTableMetadata() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                Stock.class);
    }

    private final ProductMapper productMapper = mock(ProductMapper.class);
    private final ProductSkuMapper productSkuMapper = mock(ProductSkuMapper.class);
    private final StockMapper stockMapper = mock(StockMapper.class);
    private final StockLedgerMapper stockLedgerMapper = mock(StockLedgerMapper.class);
    private final ProductServiceImpl service = new ProductServiceImpl(productMapper, mock(RedisUtil.class), productSkuMapper,
            stockMapper, stockLedgerMapper, mock(AiIndexTaskPublisher.class));

    @Test
    void rejectsPublishingProductWithoutActiveSku() {
        Product product = new Product();
        product.setStatus(2);
        when(productMapper.selectById(1L)).thenReturn(product);
        when(productSkuMapper.selectCount(any(Wrapper.class))).thenReturn(0L);

        assertThrows(BizException.class, () -> service.updateStatus(1L, 1));
    }

    @Test
    void allowsPublishingProductWithActiveSku() {
        Product product = new Product();
        product.setStatus(2);
        when(productMapper.selectById(1L)).thenReturn(product);
        when(productSkuMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        assertDoesNotThrow(() -> service.updateStatus(1L, 1));
    }

    @Test
    void updatesSkuWithoutChangingItsStock() {
        ProductSku sku = new ProductSku();
        sku.setId(2L);
        sku.setProductId(1L);
        sku.setSkuCode("PHONE-BLACK");
        when(productSkuMapper.selectById(2L)).thenReturn(sku);
        MerchantSkuUpdateDTO dto = new MerchantSkuUpdateDTO();
        dto.setSkuCode("PHONE-BLACK");
        dto.setSpecJson("{\"color\":\"black\"}");
        dto.setPrice(new java.math.BigDecimal("99.90"));
        dto.setStatus(0);

        ProductSku updated = service.updateSku(2L, dto);

        assertEquals(0, updated.getStatus());
        assertEquals(new java.math.BigDecimal("99.90"), updated.getPrice());
    }

    @Test
    void manualAdjustmentWritesCompleteInventorySnapshots() {
        ProductSku sku = new ProductSku();
        sku.setId(2L);
        sku.setProductId(1L);
        when(productSkuMapper.selectById(2L)).thenReturn(sku);
        Stock stock = new Stock();
        stock.setId(3L); stock.setSkuId(2L); stock.setTotal(10);
        stock.setLocked(3); stock.setAvailable(7); stock.setVersion(1);
        when(stockMapper.selectOne(any(Wrapper.class))).thenReturn(stock);
        when(stockMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
        StockAdjustDTO dto = new StockAdjustDTO();
        dto.setQuantity(2); dto.setReason("补货");

        service.adjustSkuStock(2L, 9L, dto);

        ArgumentCaptor<StockLedger> captor = ArgumentCaptor.forClass(StockLedger.class);
        verify(stockLedgerMapper).insert(captor.capture());
        StockLedger ledger = captor.getValue();
        assertEquals(10, ledger.getBeforeTotal());
        assertEquals(12, ledger.getAfterTotal());
        assertEquals(3, ledger.getBeforeLocked());
        assertEquals(3, ledger.getAfterLocked());
        assertEquals(7, ledger.getBeforeAvailable());
        assertEquals(9, ledger.getAfterAvailable());
    }

    @Test
    void softDeletesProductInsteadOfRemovingIt() {
        Product product = new Product();
        product.setStatus(1);
        when(productMapper.selectById(1L)).thenReturn(product);

        service.delete(1L);

        assertEquals(3, product.getStatus());
        verify(productMapper).updateById(product);
    }
}
