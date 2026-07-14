package com.biyesheji.order.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biyesheji.dto.OrderSubmitDTO;
import com.biyesheji.dto.MerchantShipmentDTO;
import com.biyesheji.vo.OrderVO;
import com.biyesheji.vo.MerchantDashboardVO;

public interface OrderService {
    String submit(Long userId, OrderSubmitDTO dto);
    OrderVO detail(Long userId, String orderNo);
    Page<OrderVO> page(Long userId, int pageNum, int pageSize, Integer status);
    void cancel(Long userId, String orderNo);
    void complete(Long userId, String orderNo);
    Page<OrderVO> merchantPage(int pageNum, int pageSize, Integer status);
    MerchantDashboardVO merchantDashboard();
    OrderVO merchantDetail(String orderNo);
    void confirmPayment(Long operatorId, String orderNo);
    void accept(Long operatorId, String orderNo);
    void ship(Long operatorId, String orderNo, MerchantShipmentDTO dto);
    void processTimeoutOrders();
}
