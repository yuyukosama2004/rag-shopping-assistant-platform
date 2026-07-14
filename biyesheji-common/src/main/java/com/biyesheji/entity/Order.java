package com.biyesheji.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_order")
public class Order extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String orderNo;
    private Long userId;
    private BigDecimal totalAmount;
    private BigDecimal productAmount;
    private BigDecimal shippingFee;
    private Long shippingRuleId;
    private String shippingRuleName;
    private String shippingMethod;
    private String paymentMethod;
    private Integer status;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String shippingCarrier;
    private String trackingNo;
    private LocalDateTime payTime;
    private LocalDateTime processingAt;
    private LocalDateTime shippedAt;
    private LocalDateTime cancelTime;
    private LocalDateTime timeoutTime;
}
