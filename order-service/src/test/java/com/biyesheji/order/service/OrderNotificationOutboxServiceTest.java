package com.biyesheji.order.service;

import com.biyesheji.entity.OrderNotificationOutbox;
import com.biyesheji.order.mapper.OrderNotificationOutboxMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderNotificationOutboxServiceTest {
    private OrderNotificationOutboxMapper mapper;
    private OrderWebhookSender sender;
    private OrderNotificationOutboxService service;

    @BeforeEach
    void setUp() {
        mapper = mock(OrderNotificationOutboxMapper.class);
        sender = mock(OrderWebhookSender.class);
        service = new OrderNotificationOutboxService(mapper, sender, new ObjectMapper());
        ReflectionTestUtils.setField(service, "maxAttempts", 6);
    }

    @Test
    void disabledWebhookDoesNotCreateOutboxRows() {
        when(sender.isEnabled()).thenReturn(false);

        service.enqueue("ORDER-1", "ORDER_CREATED", 0);

        verify(mapper, never()).insert(any(OrderNotificationOutbox.class));
    }

    @Test
    void enabledWebhookStoresMinimalEventPayload() {
        when(sender.isEnabled()).thenReturn(true);
        ArgumentCaptor<OrderNotificationOutbox> captor = ArgumentCaptor.forClass(OrderNotificationOutbox.class);

        service.enqueue("ORDER-1", "ORDER_CREATED", 0);

        verify(mapper).insert(captor.capture());
        assertEquals("ORDER-1", captor.getValue().getOrderNo());
        assertEquals("PENDING", captor.getValue().getStatus());
        assertEquals(0, captor.getValue().getAttempts());
        assertNotNull(captor.getValue().getEventId());
        assertFalse(captor.getValue().getPayload().contains("receiver"));
    }

    @Test
    void failedDeliveryIsRetriedWithBackoff() {
        OrderNotificationOutbox event = event();
        when(mapper.claim(event.getId())).thenReturn(1);
        org.mockito.Mockito.doThrow(new RuntimeException("endpoint unavailable")).when(sender).send(event);

        service.dispatch(event);

        assertEquals("RETRY", event.getStatus());
        assertEquals(1, event.getAttempts());
        assertNotNull(event.getNextAttemptAt());
        verify(mapper).updateById(event);
    }

    private OrderNotificationOutbox event() {
        OrderNotificationOutbox event = new OrderNotificationOutbox();
        event.setId(1L);
        event.setEventId("event-1");
        event.setEventType("ORDER_CREATED");
        event.setOrderNo("ORDER-1");
        event.setPayload("{\"orderNo\":\"ORDER-1\"}");
        event.setStatus("PENDING");
        event.setAttempts(0);
        return event;
    }
}
