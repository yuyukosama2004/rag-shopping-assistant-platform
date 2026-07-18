package com.biyesheji.order.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InventoryLedgerEvidenceRow {
    private String action;
    private Integer quantity;
    private Integer beforeTotal;
    private Integer afterTotal;
    private Integer beforeLocked;
    private Integer afterLocked;
    private Integer beforeAvailable;
    private Integer afterAvailable;
    private String referenceNo;
    private LocalDateTime createdAt;
}
