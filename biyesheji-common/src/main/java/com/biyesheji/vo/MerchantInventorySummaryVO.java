package com.biyesheji.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MerchantInventorySummaryVO {
    private int lowStockCount;
    private int threshold;
}
