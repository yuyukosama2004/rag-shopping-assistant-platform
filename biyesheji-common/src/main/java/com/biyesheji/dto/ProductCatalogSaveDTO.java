package com.biyesheji.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProductCatalogSaveDTO {
    @NotBlank @Size(max = 50) private String name;
    @NotNull @Min(0) private Integer sortOrder;
    @NotNull @Min(0) @Max(1) private Integer status;
}
