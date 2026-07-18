package com.biyesheji.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class InventorySaleEvidenceRow {
    private LocalDateTime paidAt;
    private Integer quantity;
    private BigDecimal subtotal;
}
