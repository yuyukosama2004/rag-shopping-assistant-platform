package com.biyesheji.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("t_shopping_cart")
public class ShoppingCart implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long productId;
    private Long skuId;
    private Integer quantity;
    private Integer checked;
    private String selectedColor;
    private String selectedStorage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
