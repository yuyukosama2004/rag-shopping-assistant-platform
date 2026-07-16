package com.biyesheji.user.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.biyesheji.constant.UserRole;
import com.biyesheji.entity.User;
import com.biyesheji.exception.BizException;
import com.biyesheji.user.mapper.AccountOrderItemMapper;
import com.biyesheji.user.mapper.AccountOrderMapper;
import com.biyesheji.user.mapper.AccountRefundMapper;
import com.biyesheji.user.mapper.AccountShoppingCartMapper;
import com.biyesheji.user.mapper.AddressMapper;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserMapper userMapper;
    @Mock private JwtUtil jwtUtil;
    @Mock private RedisUtil redisUtil;
    @Mock private AddressMapper addressMapper;
    @Mock private AccountOrderMapper accountOrderMapper;
    @Mock private AccountOrderItemMapper accountOrderItemMapper;
    @Mock private AccountRefundMapper accountRefundMapper;
    @Mock private AccountShoppingCartMapper accountShoppingCartMapper;
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

    @Test
    void accountDeletionIsBlockedWhileAnOrderIsActive() {
        User user = customer();
        when(userMapper.selectById(7L)).thenReturn(user);
        when(accountOrderMapper.selectCount(any())).thenReturn(1L);

        assertThrows(BizException.class, () -> userService.deleteAccount(7L, "correct-password", "DELETE_ACCOUNT"));

        verify(userMapper, never()).updateById(any());
        verify(addressMapper, never()).delete(any());
    }

    @Test
    void accountDeletionAnonymizesProfileAndClearsMutableCustomerData() {
        User user = customer();
        when(userMapper.selectById(7L)).thenReturn(user);
        when(accountOrderMapper.selectCount(any())).thenReturn(0L);
        when(accountRefundMapper.selectCount(any())).thenReturn(0L);

        userService.deleteAccount(7L, "correct-password", "DELETE_ACCOUNT");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        assertEquals("deleted_7", captor.getValue().getUsername());
        assertEquals("已注销用户", captor.getValue().getNickname());
        assertEquals(0, captor.getValue().getStatus());
        assertNull(captor.getValue().getEmail());
        assertNull(captor.getValue().getPhone());
        verify(addressMapper).delete(any());
        verify(accountShoppingCartMapper).delete(any());
        verify(redisUtil).delete("auth:refresh:7");
    }

    private User customer() {
        User user = new User();
        user.setId(7L);
        user.setUsername("customer");
        user.setPassword(BCrypt.hashpw("correct-password"));
        user.setNickname("消费者");
        user.setPhone("13800000000");
        user.setEmail("customer@example.test");
        user.setRole(UserRole.CUSTOMER);
        user.setStatus(1);
        return user;
    }
}
