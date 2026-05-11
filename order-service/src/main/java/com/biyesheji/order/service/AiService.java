package com.biyesheji.order.service;

import com.biyesheji.entity.AiConversation;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AiService {
    SseEmitter chat(Long userId, String query);
    AiConversation saveMessage(Long userId, String role, String content, String recommendations);
}
