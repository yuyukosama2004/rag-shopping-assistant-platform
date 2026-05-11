package com.biyesheji.order.controller;

import com.biyesheji.order.service.AiService;
import com.biyesheji.utils.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "AI导购接口", description = "对话式智能手机推荐")
@RestController
@RequestMapping("/api/order/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
    private final JwtUtil jwtUtil;

    @Operation(summary = "AI 对话（SSE流式）")
    @GetMapping(value = "/chat")
    public SseEmitter chat(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestParam String query) {
        Long userId = null;
        if (auth != null && auth.startsWith("Bearer ")) {
            try { userId = jwtUtil.getUserId(auth.replace("Bearer ", "")); } catch (Exception ignored) {}
        }
        return aiService.chat(userId, query);
    }
}
