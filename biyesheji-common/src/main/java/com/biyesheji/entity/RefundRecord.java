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
@TableName("t_refund_record")
public class RefundRecord extends BaseEntity {
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private String orderNo;
    private Long userId;
    private BigDecimal amount;
    private String reason;
    private String status;
    private String merchantNote;
    private Long processedBy;
    private LocalDateTime processedAt;
}
