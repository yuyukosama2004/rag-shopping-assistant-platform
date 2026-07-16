package com.biyesheji.order.service;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.biyesheji.entity.OrderNotificationOutbox;
import com.biyesheji.order.mapper.OrderNotificationOutboxMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.biyesheji.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderNotificationOutboxService {
    private final OrderNotificationOutboxMapper mapper;
    private final OrderWebhookSender sender;
    private final ObjectMapper objectMapper;

    @Value("${notification.webhook.max-attempts:6}")
    private int maxAttempts;

    public void enqueue(String orderNo, String eventType, int status) {
        if (!sender.isEnabled()) return;
        String eventId = UUID.randomUUID().toString();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("eventId", eventId);
        body.put("eventType", eventType);
        body.put("orderNo", orderNo);
        body.put("status", status);
        body.put("occurredAt", LocalDateTime.now().toString());
        try {
            OrderNotificationOutbox outbox = new OrderNotificationOutbox();
            outbox.setId(IdUtil.getSnowflake().nextId());
            outbox.setEventId(eventId);
            outbox.setEventType(eventType);
            outbox.setOrderNo(orderNo);
            outbox.setPayload(objectMapper.writeValueAsString(body));
            outbox.setStatus("PENDING");
            outbox.setAttempts(0);
            outbox.setNextAttemptAt(LocalDateTime.now());
            mapper.insert(outbox);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("订单通知事件序列化失败", e);
        }
    }

    public List<OrderNotificationOutbox> listRecent() {
        return mapper.selectList(new LambdaQueryWrapper<OrderNotificationOutbox>()
                .orderByDesc(OrderNotificationOutbox::getCreatedAt).last("LIMIT 100"));
    }

    public void retry(Long id) {
        OrderNotificationOutbox event = mapper.selectById(id);
        if (event == null) throw new BizException(404, "通知事件不存在");
        if (!"FAILED".equals(event.getStatus())) throw new BizException(400, "仅失败通知可以手动重试");
        event.setStatus("RETRY");
        event.setAttempts(0);
        event.setNextAttemptAt(LocalDateTime.now());
        event.setLastError(null);
        event.setDeliveredAt(null);
        mapper.updateById(event);
    }

    @Scheduled(fixedDelayString = "${notification.webhook.dispatch-delay-ms:10000}")
    public void dispatchPending() {
        if (!sender.isEnabled()) return;
        LocalDateTime now = LocalDateTime.now();
        mapper.recoverStuck(now.minusMinutes(5), now);
        List<OrderNotificationOutbox> pending = mapper.selectList(new LambdaQueryWrapper<OrderNotificationOutbox>()
                .in(OrderNotificationOutbox::getStatus, "PENDING", "RETRY")
                .le(OrderNotificationOutbox::getNextAttemptAt, now)
                .orderByAsc(OrderNotificationOutbox::getCreatedAt)
                .last("LIMIT 20"));
        for (OrderNotificationOutbox event : pending) dispatch(event);
    }

    void dispatch(OrderNotificationOutbox event) {
        if (mapper.claim(event.getId()) != 1) return;
        int attempts = event.getAttempts() + 1;
        try {
            sender.send(event);
            event.setStatus("SUCCESS");
            event.setAttempts(attempts);
            event.setDeliveredAt(LocalDateTime.now());
            event.setLastError(null);
        } catch (Exception e) {
            String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            event.setAttempts(attempts);
            event.setLastError(message.substring(0, Math.min(500, message.length())));
            if (attempts >= maxAttempts) {
                event.setStatus("FAILED");
            } else {
                event.setStatus("RETRY");
                long delaySeconds = Math.min(300, 5L * (1L << Math.min(attempts - 1, 6)));
                event.setNextAttemptAt(LocalDateTime.now().plusSeconds(delaySeconds));
            }
            log.warn("订单 Webhook 发送失败 eventId={}, orderNo={}, attempts={}", event.getEventId(), event.getOrderNo(), attempts);
        }
        mapper.updateById(event);
    }
}
