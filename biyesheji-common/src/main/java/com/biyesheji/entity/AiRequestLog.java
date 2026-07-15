package com.biyesheji.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_ai_request_log")
public class AiRequestLog extends BaseEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String model;
    private String status;
    private Integer inputChars;
    private Integer outputChars;
    private Integer estimatedInputTokens;
    private Integer estimatedOutputTokens;
    private BigDecimal estimatedCost;
    private Long durationMs;
    private String failureReason;
}
