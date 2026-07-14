package com.biyesheji.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class OrderSubmitDTO implements Serializable {
    @NotEmpty(message = "订单商品不能为空")
    @Valid
    private List<OrderItemDTO> items;
    @NotBlank(message = "收货人不能为空")
    @Size(max = 50, message = "收货人长度不能超过50")
    private String receiverName;
    @NotBlank(message = "收货电话不能为空")
    @Size(max = 20, message = "收货电话长度不能超过20")
    private String receiverPhone;
    @NotBlank(message = "收货地址不能为空")
    @Size(max = 255, message = "收货地址长度不能超过255")
    private String receiverAddress;
    private String md5;  // 前端传入的购物项MD5，用于幂等

    @Data
    public static class OrderItemDTO {
        @NotNull(message = "商品ID不能为空")
        private Long productId;
        @NotNull(message = "SKU ID不能为空")
        private Long skuId;
        @NotNull(message = "商品数量不能为空")
        @Positive(message = "商品数量必须大于0")
        private Integer quantity;
    }
}
