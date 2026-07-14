package com.biyesheji.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_order_operation")
public class OrderOperation {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String orderNo;
    private String action;
    private Long operatorId;
    private String note;
    private LocalDateTime createdAt;
}
