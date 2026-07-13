package com.biyesheji.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MerchantProductStatusDTO {
    @NotNull @Min(0) @Max(2) private Integer status;
}
