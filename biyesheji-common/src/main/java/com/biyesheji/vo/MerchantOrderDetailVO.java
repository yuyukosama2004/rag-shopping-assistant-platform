package com.biyesheji.vo;

import com.biyesheji.entity.OrderOperation;
import lombok.Data;

import java.util.List;

@Data
public class MerchantOrderDetailVO {
    private OrderVO order;
    private String merchantNote;
    private List<OrderOperation> operations;
}
