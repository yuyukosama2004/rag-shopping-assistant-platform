package com.biyesheji.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_shipping_rule")
public class ShippingRule extends BaseEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private String ruleType;
    private String name;
    private BigDecimal baseFee;
    private BigDecimal freeShippingThreshold;
    private Integer status;
    private Integer sortOrder;
}
