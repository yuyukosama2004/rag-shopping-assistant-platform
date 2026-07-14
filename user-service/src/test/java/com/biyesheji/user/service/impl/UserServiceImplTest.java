package com.biyesheji.user.service.impl;

import com.biyesheji.exception.BizException;
import com.biyesheji.user.mapper.UserMapper;
import com.biyesheji.utils.JwtUtil;
import com.biyesheji.utils.RedisUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserMapper userMapper;
    @Mock private JwtUtil jwtUtil;
    @Mock private RedisUtil redisUtil;
    @InjectMocks private UserServiceImpl userService;

    @Test
    void fifthFailedLoginLocksTheUsername() {
        when(redisUtil.exists(anyString())).thenReturn(false);
        when(userMapper.selectOne(any())).thenReturn(null);
        when(redisUtil.increment(anyString())).thenReturn(5L);

        assertThrows(BizException.class, () -> userService.login("alice", "wrong-password"));

        verify(redisUtil).set(anyString(), eq("locked"), eq(15L), eq(TimeUnit.MINUTES));
        verify(redisUtil).delete(anyString());
    }

    @Test
    void lockedUsernameDoesNotQueryCredentials() {
        when(redisUtil.exists(anyString())).thenReturn(true);

        assertThrows(BizException.class, () -> userService.login("alice", "wrong-password"));

        verify(userMapper, never()).selectOne(any());
        verify(redisUtil, never()).increment(anyString());
    }
}
