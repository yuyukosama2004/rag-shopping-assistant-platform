package com.biyesheji.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_order_notification_outbox")
public class OrderNotificationOutbox extends BaseEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String eventId;
    private String eventType;
    private String orderNo;
    private String payload;
    private String status;
    private Integer attempts;
    private LocalDateTime nextAttemptAt;
    private String lastError;
    private LocalDateTime deliveredAt;
}
