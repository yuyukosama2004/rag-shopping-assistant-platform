package com.biyesheji.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_stock_ledger")
public class StockLedger {
    @TableId(type = IdType.ASSIGN_ID) private Long id;
    private Long skuId;
    private String action;
    private Integer quantity;
    private Integer beforeAvailable;
    private Integer afterAvailable;
    private Long operatorId;
    private String referenceNo;
    private LocalDateTime createdAt;
}
