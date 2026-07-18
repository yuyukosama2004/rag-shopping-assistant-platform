package com.biyesheji.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class InventoryInsightRow {
    private Long productId;
    private String productName;
    private Long skuId;
    private String skuCode;
    private String skuSpecJson;
    private Integer productStatus;
    private Integer skuStatus;
    private Integer total;
    private Integer locked;
    private Integer available;
    private Long confirmedQty7d;
    private Long confirmedQty30d;
    private Long confirmedQty90d;
    private Long demandQty30d;
    private BigDecimal confirmedRevenue30d;
    private LocalDateTime lastConfirmedSaleAt;
    private LocalDateTime skuCreatedAt;
}
