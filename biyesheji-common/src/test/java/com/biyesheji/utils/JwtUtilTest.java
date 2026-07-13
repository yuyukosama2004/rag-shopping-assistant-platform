package com.biyesheji.utils;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtUtilTest {

    @Test
    void accessAndRefreshTokensHaveSeparateTypesAndIds() {
        JwtUtil jwtUtil = configuredJwtUtil();
        String accessToken = jwtUtil.generateAccessToken(42L, "alice");
        String refreshToken = jwtUtil.generateRefreshToken(42L, "alice");

        assertEquals(JwtUtil.ACCESS_TOKEN, jwtUtil.getTokenType(accessToken));
        assertEquals(JwtUtil.REFRESH_TOKEN, jwtUtil.getTokenType(refreshToken));
        assertEquals(42L, jwtUtil.getUserId(accessToken));
        assertNotNull(jwtUtil.getTokenId(accessToken));
        assertNotNull(jwtUtil.getTokenId(refreshToken));
    }

    @Test
    void startupRejectsShortSecrets() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "too-short");
        assertThrows(IllegalStateException.class, jwtUtil::validateSecret);
    }

    @Test
    void refreshTokenCannotAuthenticateBusinessRequest() {
        JwtUtil jwtUtil = configuredJwtUtil();
        String refreshToken = jwtUtil.generateRefreshToken(42L, "alice");
        assertThrows(RuntimeException.class, () -> jwtUtil.getAccessUserId(refreshToken));
    }

    private JwtUtil configuredJwtUtil() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "a-secure-test-secret-that-is-long-enough");
        ReflectionTestUtils.setField(jwtUtil, "accessTokenExpire", 7200L);
        ReflectionTestUtils.setField(jwtUtil, "refreshTokenExpire", 604800L);
        jwtUtil.validateSecret();
        return jwtUtil;
    }
}
