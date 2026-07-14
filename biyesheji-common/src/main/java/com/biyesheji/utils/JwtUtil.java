package com.biyesheji.utils;

import com.biyesheji.exception.BizException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtUtil {

    public static final String ACCESS_TOKEN = "access";
    public static final String REFRESH_TOKEN = "refresh";
    private static final String TOKEN_TYPE_CLAIM = "tokenType";

    @Value("${jwt.secret:}")
    private String secret;

    @Value("${jwt.access-token-expire:7200}")
    private Long accessTokenExpire;

    @Value("${jwt.refresh-token-expire:604800}")
    private Long refreshTokenExpire;

    @PostConstruct
    void validateSecret() {
        if (!StringUtils.hasText(secret) || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 bytes");
        }
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Long userId, String username) {
        return generateToken(userId, username, accessTokenExpire, ACCESS_TOKEN);
    }

    public String generateAccessToken(Long userId, String username, Integer role) {
        return generateToken(userId, username, accessTokenExpire, ACCESS_TOKEN, role);
    }

    public String generateRefreshToken(Long userId, String username) {
        return generateToken(userId, username, refreshTokenExpire, REFRESH_TOKEN);
    }

    public String generateRefreshToken(Long userId, String username, Integer role) {
        return generateToken(userId, username, refreshTokenExpire, REFRESH_TOKEN, role);
    }

    private String generateToken(Long userId, String username, Long expireSeconds, String tokenType) {
        return generateToken(userId, username, expireSeconds, tokenType, null);
    }

    private String generateToken(Long userId, String username, Long expireSeconds, String tokenType, Integer role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put(TOKEN_TYPE_CLAIM, tokenType);
        if (role != null) claims.put("role", role);

        Date now = new Date();
        Date expiration = new Date(now.getTime() + expireSeconds * 1000);
        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getKey())
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserId(String token) {
        return parseToken(token).get("userId", Long.class);
    }

    public Long getAccessUserId(String token) {
        if (!ACCESS_TOKEN.equals(getTokenType(token))) {
            throw new BizException(401, "仅允许使用访问令牌");
        }
        return getUserId(token);
    }

    public String getUsername(String token) {
        return parseToken(token).getSubject();
    }

    public String getTokenType(String token) {
        return parseToken(token).get(TOKEN_TYPE_CLAIM, String.class);
    }

    public String getTokenId(String token) {
        return parseToken(token).getId();
    }

    public boolean isExpired(String token) {
        try {
            return parseToken(token).getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    public Long getAccessTokenExpire() {
        return accessTokenExpire;
    }

    public Long getRefreshTokenExpire() {
        return refreshTokenExpire;
    }
}
