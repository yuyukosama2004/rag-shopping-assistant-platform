package com.biyesheji.order.service;

import com.biyesheji.order.dto.InventoryInsightView;

public interface InventoryInsightService {

    InventoryInsightView.Page list(
            int pageNum,
            int pageSize,
            String keyword,
            InventoryInsightView.Risk risk,
            InventoryInsightView.Sort sort
    );

    InventoryInsightView.Summary summary();

    InventoryInsightView.Evidence evidence(Long skuId);
}
