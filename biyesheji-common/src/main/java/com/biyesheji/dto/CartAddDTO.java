package com.biyesheji.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CartAddDTO {
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotNull(message = "商品数量不能为空")
    @Positive(message = "商品数量必须大于0")
    private Integer quantity = 1;

    @Size(max = 50, message = "颜色长度不能超过50")
    private String color;

    @Size(max = 50, message = "存储规格长度不能超过50")
    private String storage;
}
