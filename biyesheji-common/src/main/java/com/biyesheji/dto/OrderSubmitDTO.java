package com.biyesheji.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class OrderSubmitDTO implements Serializable {
    private List<OrderItemDTO> items;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String md5;  // 前端传入的购物项MD5，用于幂等

    @Data
    public static class OrderItemDTO {
        private Long productId;
        private Integer quantity;
    }
}
