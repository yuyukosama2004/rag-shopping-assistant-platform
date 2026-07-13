package com.biyesheji.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class MerchantProductSaveDTO {
    @NotBlank @Size(max = 200) private String name;
    @NotBlank @Size(max = 50) private String brand;
    @NotBlank @Size(max = 50) private String category;
    @NotNull @DecimalMin("0.01") private BigDecimal price;
    @DecimalMin("0.01") private BigDecimal originalPrice;
    private String specJson;
    @Size(max = 255) private String mainImage;
    private String images;
    private String description;
    private String colorOptions;
    private String storageOptions;
}
