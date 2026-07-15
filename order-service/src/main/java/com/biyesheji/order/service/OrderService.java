package com.biyesheji.order.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biyesheji.dto.OrderSubmitDTO;
import com.biyesheji.dto.MerchantShipmentDTO;
import com.biyesheji.vo.OrderVO;
import com.biyesheji.vo.MerchantDashboardVO;
import com.biyesheji.vo.MerchantOrderDetailVO;

public interface OrderService {
    String submit(Long userId, OrderSubmitDTO dto);
    OrderVO detail(Long userId, String orderNo);
    Page<OrderVO> page(Long userId, int pageNum, int pageSize, Integer status);
    void cancel(Long userId, String orderNo);
    void complete(Long userId, String orderNo);
    Page<OrderVO> merchantPage(int pageNum, int pageSize, Integer status);
    MerchantDashboardVO merchantDashboard();
    MerchantOrderDetailVO merchantDetail(String orderNo);
    void updateMerchantNote(Long operatorId, String orderNo, String note);
    void close(Long operatorId, String orderNo, String reason);
    void confirmPayment(Long operatorId, String orderNo);
    void accept(Long operatorId, String orderNo);
    void ship(Long operatorId, String orderNo, MerchantShipmentDTO dto);
    void processTimeoutOrders();
}
