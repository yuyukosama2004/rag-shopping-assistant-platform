package com.biyesheji.order.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biyesheji.dto.OrderSubmitDTO;
import com.biyesheji.vo.OrderVO;

import java.util.Map;

public interface OrderService {
    /** 下单 - 高并发异步链路 */
    String submit(Long userId, OrderSubmitDTO dto);
    /** MQ 消费端 - 真正落库 */
    void processOrder(Map<String, Object> msg);
    /** 订单详情 */
    OrderVO detail(Long userId, String orderNo);
    /** 用户订单分页 */
    Page<OrderVO> page(Long userId, int pageNum, int pageSize, Integer status);
    /** 模拟支付 */
    void pay(Long userId, String orderNo);
    /** 取消订单 */
    void cancel(Long userId, String orderNo);
    /** 超时补偿 - 定时任务 */
    void processTimeoutOrders();
    /** 创建测试订单 - 方便前端调试 */
    String createTestOrder(Long userId, Long productId, Integer quantity);
}
