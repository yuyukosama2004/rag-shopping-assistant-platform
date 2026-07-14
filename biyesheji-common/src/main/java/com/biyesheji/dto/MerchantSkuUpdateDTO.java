package com.biyesheji.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class MerchantSkuUpdateDTO {
    @NotBlank @Size(max = 64) private String skuCode;
    @Size(max = 2000) private String specJson;
    @NotNull @DecimalMin("0.01") private BigDecimal price;
    @DecimalMin("0.01") private BigDecimal originalPrice;
    @NotNull @Min(0) @Max(1) private Integer status;
}
