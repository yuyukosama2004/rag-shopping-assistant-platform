package com.biyesheji.order.task;

import com.biyesheji.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 订单超时补偿定时任务 — 每5分钟扫描一次
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class OrderTimeoutTask {

    private final OrderService orderService;

    @Scheduled(fixedRate = 300000)  // 每5分钟
    public void processTimeoutOrders() {
        log.debug("开始扫描超时订单...");
        orderService.processTimeoutOrders();
    }
}
