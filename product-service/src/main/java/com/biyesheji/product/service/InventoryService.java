package com.biyesheji.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biyesheji.vo.MerchantInventorySummaryVO;
import com.biyesheji.vo.MerchantInventoryVO;

public interface InventoryService {
    Page<MerchantInventoryVO> page(int pageNum, int pageSize, String keyword, boolean lowStockOnly, int threshold);
    MerchantInventorySummaryVO summary(int threshold);
}
