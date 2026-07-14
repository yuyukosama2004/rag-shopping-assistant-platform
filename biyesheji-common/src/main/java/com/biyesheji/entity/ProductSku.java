package com.biyesheji.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_product_sku")
public class ProductSku {
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private Long productId;
    private String skuCode;
    private String specJson;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer status;
    @TableField(exist = false) private Integer available;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
