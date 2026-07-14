package com.biyesheji.order.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biyesheji.dto.OrderSubmitDTO;
import com.biyesheji.vo.OrderVO;

public interface OrderService {
    String submit(Long userId, OrderSubmitDTO dto);
    OrderVO detail(Long userId, String orderNo);
    Page<OrderVO> page(Long userId, int pageNum, int pageSize, Integer status);
    void pay(Long userId, String orderNo);
    void cancel(Long userId, String orderNo);
    void processTimeoutOrders();
}
