package com.biyesheji.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class MerchantProductBatchStatusDTO {
    @NotEmpty private List<Long> ids;
    @NotNull @Min(0) @Max(2) private Integer status;
}
