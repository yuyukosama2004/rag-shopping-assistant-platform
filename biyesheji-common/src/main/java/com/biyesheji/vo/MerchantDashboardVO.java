package com.biyesheji.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MerchantDashboardVO {
    private long pendingOrderCount;
    private long todayOrderCount;
    private BigDecimal todayConfirmedSales;
}
