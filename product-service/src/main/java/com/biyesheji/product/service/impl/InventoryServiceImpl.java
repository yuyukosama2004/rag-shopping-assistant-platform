package com.biyesheji.product.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biyesheji.product.mapper.InventoryMapper;
import com.biyesheji.product.service.InventoryService;
import com.biyesheji.vo.MerchantInventorySummaryVO;
import com.biyesheji.vo.MerchantInventoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {
    private final InventoryMapper inventoryMapper;

    @Override
    public Page<MerchantInventoryVO> page(int pageNum, int pageSize, String keyword, boolean lowStockOnly, int threshold) {
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return inventoryMapper.selectInventoryPage(
                new Page<>(pageNum, pageSize), normalizedKeyword, lowStockOnly, threshold);
    }

    @Override
    public MerchantInventorySummaryVO summary(int threshold) {
        return new MerchantInventorySummaryVO(inventoryMapper.countLowStock(threshold), threshold);
    }
}
