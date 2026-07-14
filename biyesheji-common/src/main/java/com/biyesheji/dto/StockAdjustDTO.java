package com.biyesheji.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StockAdjustDTO {
    @NotNull(message = "调整数量不能为空")
    private Integer quantity;

    @Size(max = 64, message = "调整原因不能超过64个字符")
    private String reason;
}
