package com.biyesheji.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ShippingRuleSaveDTO {
    @NotBlank @Pattern(regexp = "DELIVERY|PICKUP") private String ruleType;
    @NotBlank @Size(max = 50) private String name;
    @NotNull @DecimalMin("0.00") private BigDecimal baseFee;
    @DecimalMin("0.01") private BigDecimal freeShippingThreshold;
    @NotNull @Min(0) @Max(1) private Integer status;
    @NotNull @Min(0) private Integer sortOrder;
}
