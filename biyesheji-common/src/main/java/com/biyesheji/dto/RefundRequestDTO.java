package com.biyesheji.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RefundRequestDTO {
    @NotNull @DecimalMin("0.01") private BigDecimal amount;
    @NotBlank @Size(max = 500) private String reason;
}
