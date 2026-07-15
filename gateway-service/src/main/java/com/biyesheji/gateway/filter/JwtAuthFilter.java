package com.biyesheji.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final String ACCESS_TOKEN = "access";

    @Value("${jwt.secret:}")
    private String secret;

    private static final List<String> WHITE_LIST = List.of(
            "/api/user/login",
            "/api/user/register",
            "/api/user/refresh",
            "/api/product",
            "/api/media",
            "/api/shipping-rules",
            "/api/store"
    );

    @PostConstruct
    void validateSecret() {
        if (!StringUtils.hasText(secret) || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 bytes");
        }
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        String path = exchange.getRequest().getURI().getPath();
        if ("/api/merchant/initialize".equals(path) || WHITE_LIST.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "未登录");
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(authHeader.substring(7))
                    .getPayload();
            if (!ACCESS_TOKEN.equals(claims.get("tokenType", String.class))) {
                return unauthorized(exchange, "Token类型无效");
            }

            Long userId = claims.get("userId", Long.class);
            if (userId == null) {
                return unauthorized(exchange, "Token缺少用户信息");
            }
            ServerHttpRequest request = exchange.getRequest().mutate()
                    .headers(headers -> {
                        headers.remove("X-User-Id");
                        headers.remove("X-Username");
                        headers.remove("X-User-Role");
                        headers.set("X-User-Id", userId.toString());
                        headers.set("X-Username", claims.getSubject());
                        headers.set("X-User-Role", String.valueOf(claims.get("role", Integer.class)));
                    })
                    .build();
            return chain.filter(exchange.mutate().request(request).build());
        } catch (Exception e) {
            log.warn("Token校验失败: {}", e.getMessage());
            return unauthorized(exchange, "Token无效或已过期");
        }
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"code\":401,\"message\":\"" + message + "\",\"data\":null}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
