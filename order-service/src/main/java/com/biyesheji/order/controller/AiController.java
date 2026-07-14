package com.biyesheji.order.controller;

import com.biyesheji.order.service.AiService;
import com.biyesheji.utils.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.validation.annotation.Validated;

@Tag(name = "AI导购接口", description = "对话式智能手机推荐")
@RestController
@RequestMapping("/api/order/ai")
@RequiredArgsConstructor
@Validated
public class AiController {

    private final AiService aiService;
    private final JwtUtil jwtUtil;

    @Operation(summary = "AI对话（SSE流式）")
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestHeader("Authorization") String auth,
                           @RequestParam @NotBlank(message = "咨询内容不能为空")
                           @Size(max = 500, message = "咨询内容不能超过500个字符") String query) {
        Long userId = jwtUtil.getAccessUserId(auth.replace("Bearer ", ""));
        return aiService.chat(userId, query.trim());
    }
}
