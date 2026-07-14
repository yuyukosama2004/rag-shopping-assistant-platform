package com.biyesheji.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CartBatchDeleteDTO {
    @NotEmpty(message = "购物车ID不能为空")
    private List<@NotNull(message = "购物车ID不能为空") Long> ids;
}
