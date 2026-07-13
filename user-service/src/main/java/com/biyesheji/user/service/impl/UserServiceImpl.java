package com.biyesheji.user.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.biyesheji.constant.ResultCode;
import com.biyesheji.constant.UserRole;
import com.biyesheji.dto.RegisterDTO;
import com.biyesheji.entity.User;
import com.biyesheji.exception.BizException;
import com.biyesheji.user.mapper.UserMapper;
import com.biyesheji.user.service.UserService;
import com.biyesheji.utils.JwtUtil;
import com.biyesheji.utils.RedisUtil;
import com.biyesheji.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String REFRESH_TOKEN_KEY_PREFIX = "auth:refresh:";

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;

    @Override
    public LoginVO login(String username, String password) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (user == null || !BCrypt.checkpw(password, user.getPassword())) {
            throw new BizException(ResultCode.PASSWORD_ERROR, "用户名或密码错误");
        }
        ensureEnabled(user);
        return issueTokens(user);
    }

    @Override
    public User register(RegisterDTO dto) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new BizException(ResultCode.USER_EXISTS, "用户名已存在");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(BCrypt.hashpw(dto.getPassword()));
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setRole(UserRole.CUSTOMER);
        user.setStatus(1);
        userMapper.insert(user);
        user.setPassword(null);
        return user;
    }

    @Override
    public User getById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND, "用户不存在");
        }
        user.setPassword(null);
        return user;
    }

    @Override
    public User updateInfo(Long userId, User updateUser) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND, "用户不存在");
        }
        if (updateUser.getNickname() != null) user.setNickname(updateUser.getNickname());
        if (updateUser.getPhone() != null) user.setPhone(updateUser.getPhone());
        if (updateUser.getEmail() != null) user.setEmail(updateUser.getEmail());
        if (updateUser.getAvatar() != null) user.setAvatar(updateUser.getAvatar());
        userMapper.updateById(user);
        user.setPassword(null);
        return user;
    }

    @Override
    public LoginVO refreshToken(String refreshToken) {
        if (jwtUtil.isExpired(refreshToken) || !JwtUtil.REFRESH_TOKEN.equals(jwtUtil.getTokenType(refreshToken))) {
            throw new BizException(ResultCode.TOKEN_EXPIRED, "刷新令牌无效或已过期");
        }
        Long userId = jwtUtil.getUserId(refreshToken);
        String tokenId = jwtUtil.getTokenId(refreshToken);
        String storedTokenId = redisUtil.get(refreshTokenKey(userId));
        if (!Objects.equals(tokenId, storedTokenId)) {
            throw new BizException(ResultCode.TOKEN_EXPIRED, "刷新令牌已失效");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND, "用户不存在");
        }
        ensureEnabled(user);
        return issueTokens(user);
    }

    @Override
    public void logout(Long userId) {
        redisUtil.delete(refreshTokenKey(userId));
    }

    private LoginVO issueTokens(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());
        redisUtil.set(refreshTokenKey(user.getId()), jwtUtil.getTokenId(refreshToken),
                jwtUtil.getRefreshTokenExpire(), TimeUnit.SECONDS);
        return LoginVO.of(accessToken, refreshToken, user.getId(), user.getUsername(),
                user.getNickname(), jwtUtil.getAccessTokenExpire());
    }

    private void ensureEnabled(User user) {
        if (user.getStatus() == null || user.getStatus() == 0) {
            throw new BizException(ResultCode.FORBIDDEN, "账号已被禁用");
        }
    }

    private String refreshTokenKey(Long userId) {
        return REFRESH_TOKEN_KEY_PREFIX + userId;
    }
}
