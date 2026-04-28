package com.biyesheji.order.service;

import com.biyesheji.entity.AiConversation;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

public interface AiService {
    Flux<ServerSentEvent<String>> chat(Long userId, String query);
    AiConversation saveMessage(Long userId, String role, String content, String recommendations);
}
