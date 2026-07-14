package com.biyesheji.order.service;

import com.biyesheji.constant.OrderStatus;
import com.biyesheji.dto.RefundRequestDTO;
import com.biyesheji.entity.Order;
import com.biyesheji.order.mapper.OrderMapper;
import com.biyesheji.order.mapper.OrderOperationMapper;
import com.biyesheji.order.mapper.RefundRecordMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {
    @Mock private RefundRecordMapper refundRecordMapper;
    @Mock private OrderMapper orderMapper;
    @Mock private OrderOperationMapper orderOperationMapper;
    @InjectMocks private RefundService refundService;

    @Test
    void rejectsRefundRequestBeforePaymentIsConfirmed() {
        Order order = new Order();
        order.setOrderNo("ORDER-1"); order.setUserId(7L); order.setStatus(OrderStatus.PENDING.getCode()); order.setTotalAmount(new BigDecimal("100.00"));
        when(orderMapper.selectOne(any())).thenReturn(order);
        RefundRequestDTO dto = new RefundRequestDTO(); dto.setAmount(new BigDecimal("10.00")); dto.setReason("不需要了");

        assertThrows(RuntimeException.class, () -> refundService.request(7L, "ORDER-1", dto));
    }
}
