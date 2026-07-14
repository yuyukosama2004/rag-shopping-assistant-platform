package com.biyesheji.constant;

import lombok.Getter;

@Getter
public enum OrderStatus {

    PENDING(0, "待商家确认"),
    PAID(1, "已支付"),
    SHIPPED(2, "已发货"),
    COMPLETED(3, "已完成"),
    CANCELLED(4, "已取消"),
    TIMEOUT(5, "已超时"),
    PROCESSING(6, "处理中");

    private final int code;
    private final String desc;

    OrderStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String descriptionOf(Integer code) {
        for (OrderStatus status : values()) if (status.code == code) return status.desc;
        return "未知状态";
    }
}
