package com.biyesheji.product.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biyesheji.product.mapper.InventoryMapper;
import com.biyesheji.vo.MerchantInventoryVO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InventoryServiceImplTest {
    private final InventoryMapper inventoryMapper = mock(InventoryMapper.class);
    private final InventoryServiceImpl service = new InventoryServiceImpl(inventoryMapper);

    @Test
    void normalizesKeywordAndAppliesLowStockThreshold() {
        when(inventoryMapper.selectInventoryPage(any(), any(), eq(true), eq(5)))
                .thenReturn(new Page<>(2, 30));

        service.page(2, 30, "  PHONE-01  ", true, 5);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Page<MerchantInventoryVO>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(inventoryMapper).selectInventoryPage(pageCaptor.capture(), eq("PHONE-01"), eq(true), eq(5));
        assertEquals(2, pageCaptor.getValue().getCurrent());
        assertEquals(30, pageCaptor.getValue().getSize());
    }

    @Test
    void returnsConfiguredThresholdWithLowStockCount() {
        when(inventoryMapper.countLowStock(8)).thenReturn(3);

        var summary = service.summary(8);

        assertEquals(3, summary.getLowStockCount());
        assertEquals(8, summary.getThreshold());
    }
}
