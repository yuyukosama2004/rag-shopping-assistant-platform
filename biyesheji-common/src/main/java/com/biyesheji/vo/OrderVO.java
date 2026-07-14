package com.biyesheji.vo;

import com.biyesheji.entity.Order;
import com.biyesheji.entity.OrderItem;
import com.biyesheji.constant.OrderStatus;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderVO implements Serializable {
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
    private String statusDesc;
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
    private LocalDateTime createdAt;
    private List<OrderItem> items;

    public static OrderVO from(Order order, List<OrderItem> items) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setProductAmount(order.getProductAmount());
        vo.setShippingFee(order.getShippingFee());
        vo.setShippingRuleId(order.getShippingRuleId());
        vo.setShippingRuleName(order.getShippingRuleName());
        vo.setShippingMethod(order.getShippingMethod());
        vo.setPaymentMethod(order.getPaymentMethod());
        vo.setStatus(order.getStatus());
        vo.setStatusDesc(OrderStatus.descriptionOf(order.getStatus()));
        vo.setReceiverName(order.getReceiverName());
        vo.setReceiverPhone(order.getReceiverPhone());
        vo.setReceiverAddress(order.getReceiverAddress());
        vo.setShippingCarrier(order.getShippingCarrier());
        vo.setTrackingNo(order.getTrackingNo());
        vo.setPayTime(order.getPayTime());
        vo.setProcessingAt(order.getProcessingAt());
        vo.setShippedAt(order.getShippedAt());
        vo.setCancelTime(order.getCancelTime());
        vo.setTimeoutTime(order.getTimeoutTime());
        vo.setCreatedAt(order.getCreatedAt());
        vo.setItems(items);
        return vo;
    }
}
