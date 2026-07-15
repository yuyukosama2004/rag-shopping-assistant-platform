package com.biyesheji.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_ai_setting")
public class AiSetting extends BaseEntity {
    @TableId(type = IdType.INPUT)
    private Long id;
    private Integer enabled;
    private String model;
    private BigDecimal temperature;
    private Integer maxOutputTokens;
    private Integer perUserDailyLimit;
    private BigDecimal dailyBudget;
    private BigDecimal inputPricePerMillion;
    private BigDecimal outputPricePerMillion;
    private String blockedKeywords;
    private String disclaimer;
    private String systemPrompt;
}
