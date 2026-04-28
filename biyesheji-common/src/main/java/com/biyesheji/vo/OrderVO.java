package com.biyesheji.vo;

import com.biyesheji.entity.Order;
import com.biyesheji.entity.OrderItem;
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
    private Integer status;
    private String statusDesc;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private LocalDateTime payTime;
    private LocalDateTime cancelTime;
    private LocalDateTime timeoutTime;
    private LocalDateTime createdAt;
    private List<OrderItem> items;

    private static final String[] STATUS_DESC = {"待支付", "已支付", "已发货", "已完成", "已取消", "已超时"};

    public static OrderVO from(Order order, List<OrderItem> items) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setStatus(order.getStatus());
        vo.setStatusDesc(STATUS_DESC[order.getStatus()]);
        vo.setReceiverName(order.getReceiverName());
        vo.setReceiverPhone(order.getReceiverPhone());
        vo.setReceiverAddress(order.getReceiverAddress());
        vo.setPayTime(order.getPayTime());
        vo.setCancelTime(order.getCancelTime());
        vo.setTimeoutTime(order.getTimeoutTime());
        vo.setCreatedAt(order.getCreatedAt());
        vo.setItems(items);
        return vo;
    }
}
