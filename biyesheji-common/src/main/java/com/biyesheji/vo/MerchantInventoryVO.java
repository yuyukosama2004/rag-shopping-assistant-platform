package com.biyesheji.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MerchantInventoryVO {
    private Long productId;
    private String productName;
    private Integer productStatus;
    private Long skuId;
    private String skuCode;
    private String specJson;
    private Integer skuStatus;
    private Integer total;
    private Integer locked;
    private Integer available;
    private LocalDateTime updatedAt;
}
