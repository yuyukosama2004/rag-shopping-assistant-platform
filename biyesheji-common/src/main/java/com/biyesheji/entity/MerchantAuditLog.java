package com.biyesheji.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_merchant_audit_log")
public class MerchantAuditLog {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long operatorId;
    private String action;
    private String resourceType;
    private Long resourceId;
    private String result;
    private LocalDateTime createdAt;
}
