package com.biyesheji.user.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.biyesheji.constant.ResultCode;
import com.biyesheji.constant.UserRole;
import com.biyesheji.dto.RegisterDTO;
import com.biyesheji.entity.User;
import com.biyesheji.entity.Address;
import com.biyesheji.entity.Order;
import com.biyesheji.entity.OrderItem;
import com.biyesheji.entity.RefundRecord;
import com.biyesheji.entity.ShoppingCart;
import com.biyesheji.exception.BizException;
import com.biyesheji.user.mapper.UserMapper;
import com.biyesheji.user.mapper.AccountOrderItemMapper;
import com.biyesheji.user.mapper.AccountOrderMapper;
import com.biyesheji.user.mapper.AccountRefundMapper;
import com.biyesheji.user.mapper.AccountShoppingCartMapper;
import com.biyesheji.user.mapper.AddressMapper;
import com.biyesheji.user.service.UserService;
import com.biyesheji.user.vo.AccountDataExportVO;
import com.biyesheji.utils.JwtUtil;
import com.biyesheji.utils.RedisUtil;
import com.biyesheji.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String REFRESH_TOKEN_KEY_PREFIX = "auth:refresh:";
    private static final String LOGIN_FAILURE_KEY_PREFIX = "auth:login:fail:";
    private static final String LOGIN_LOCK_KEY_PREFIX = "auth:login:lock:";
    private static final int MAX_LOGIN_FAILURES = 5;

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;
    private final AddressMapper addressMapper;
    private final AccountOrderMapper accountOrderMapper;
    private final AccountOrderItemMapper accountOrderItemMapper;
    private final AccountRefundMapper accountRefundMapper;
    private final AccountShoppingCartMapper accountShoppingCartMapper;

    @Override
    public LoginVO login(String username, String password) {
        if (redisUtil.exists(loginLockKey(username))) {
            throw new BizException(ResultCode.FORBIDDEN, "登录尝试过于频繁，请15分钟后重试");
        }
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (user == null || !BCrypt.checkpw(password, user.getPassword())) {
            recordLoginFailure(username);
            throw new BizException(ResultCode.PASSWORD_ERROR, "用户名或密码错误");
        }
        ensureEnabled(user);
        redisUtil.delete(loginFailureKey(username));
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

    @Override
    public AccountDataExportVO exportAccountData(Long userId) {
        User user = requireCustomer(userId);
        List<Address> addresses = addressMapper.selectList(new LambdaQueryWrapper<Address>()
                .eq(Address::getUserId, userId).orderByDesc(Address::getCreatedAt));
        List<Order> orders = accountOrderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId).orderByDesc(Order::getCreatedAt));
        List<String> orderNos = orders.stream().map(Order::getOrderNo).toList();
        Map<String, List<OrderItem>> itemsByOrder = orderNos.isEmpty() ? Collections.emptyMap()
                : accountOrderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderNo, orderNos)).stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderNo));
        Map<String, List<RefundRecord>> refundsByOrder = orderNos.isEmpty() ? Collections.emptyMap()
                : accountRefundMapper.selectList(new LambdaQueryWrapper<RefundRecord>().eq(RefundRecord::getUserId, userId)).stream()
                .collect(Collectors.groupingBy(RefundRecord::getOrderNo));

        return AccountDataExportVO.builder()
                .generatedAt(LocalDateTime.now())
                .profile(AccountDataExportVO.Profile.builder()
                        .id(user.getId()).username(user.getUsername()).nickname(user.getNickname())
                        .phone(user.getPhone()).email(user.getEmail()).avatar(user.getAvatar()).createdAt(user.getCreatedAt()).build())
                .addresses(addresses)
                .orders(orders.stream().map(order -> exportOrder(order,
                        itemsByOrder.getOrDefault(order.getOrderNo(), List.of()),
                        refundsByOrder.getOrDefault(order.getOrderNo(), List.of()))).toList())
                .build();
    }

    @Override
    @Transactional
    public void deleteAccount(Long userId, String password, String confirmation) {
        if (!"DELETE_ACCOUNT".equals(confirmation)) throw new BizException(400, "注销确认无效");
        User user = requireCustomer(userId);
        if (!BCrypt.checkpw(password, user.getPassword())) throw new BizException(ResultCode.PASSWORD_ERROR, "当前密码错误");
        if (accountOrderMapper.selectCount(new LambdaQueryWrapper<Order>().eq(Order::getUserId, userId)
                .notIn(Order::getStatus, List.of(3, 4, 5))) > 0) {
            throw new BizException(409, "仍有进行中的订单，请完成或取消后再注销");
        }
        if (accountRefundMapper.selectCount(new LambdaQueryWrapper<RefundRecord>().eq(RefundRecord::getUserId, userId)
                .eq(RefundRecord::getStatus, "PENDING")) > 0) {
            throw new BizException(409, "仍有待处理退款，请处理完成后再注销");
        }

        addressMapper.delete(new LambdaQueryWrapper<Address>().eq(Address::getUserId, userId));
        accountShoppingCartMapper.delete(new LambdaQueryWrapper<ShoppingCart>().eq(ShoppingCart::getUserId, userId));
        user.setUsername("deleted_" + userId);
        user.setPassword(BCrypt.hashpw(java.util.UUID.randomUUID().toString()));
        user.setNickname("已注销用户");
        user.setPhone(null);
        user.setEmail(null);
        user.setAvatar(null);
        user.setStatus(0);
        userMapper.updateById(user);
        redisUtil.delete(refreshTokenKey(userId));
        log.info("消费者账户已注销并匿名化 userId={}", userId);
    }

    private User requireCustomer(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BizException(ResultCode.USER_NOT_FOUND, "用户不存在");
        if (!Integer.valueOf(UserRole.CUSTOMER).equals(user.getRole())) throw new BizException(ResultCode.FORBIDDEN, "仅消费者账户支持此操作");
        if (user.getStatus() == null || user.getStatus() == 0) throw new BizException(ResultCode.FORBIDDEN, "账号已注销或禁用");
        return user;
    }

    private AccountDataExportVO.ExportedOrder exportOrder(Order order, List<OrderItem> items, List<RefundRecord> refunds) {
        return AccountDataExportVO.ExportedOrder.builder()
                .orderNo(order.getOrderNo()).productAmount(order.getProductAmount()).shippingFee(order.getShippingFee())
                .totalAmount(order.getTotalAmount()).shippingRuleName(order.getShippingRuleName()).shippingMethod(order.getShippingMethod())
                .paymentMethod(order.getPaymentMethod()).status(order.getStatus()).receiverName(order.getReceiverName())
                .receiverPhone(order.getReceiverPhone()).receiverAddress(order.getReceiverAddress())
                .shippingCarrier(order.getShippingCarrier()).trackingNo(order.getTrackingNo()).payTime(order.getPayTime())
                .processingAt(order.getProcessingAt()).shippedAt(order.getShippedAt()).cancelTime(order.getCancelTime())
                .createdAt(order.getCreatedAt())
                .items(items.stream().map(item -> AccountDataExportVO.ExportedOrderItem.builder()
                        .productId(item.getProductId()).skuId(item.getSkuId()).skuCode(item.getSkuCode()).skuSpecJson(item.getSkuSpecJson())
                        .productName(item.getProductName()).productImage(item.getProductImage()).price(item.getPrice())
                        .quantity(item.getQuantity()).subtotal(item.getSubtotal()).build()).toList())
                .refunds(refunds.stream().map(refund -> AccountDataExportVO.ExportedRefund.builder()
                        .amount(refund.getAmount()).reason(refund.getReason()).status(refund.getStatus())
                        .processedAt(refund.getProcessedAt()).createdAt(refund.getCreatedAt()).build()).toList())
                .build();
    }

    private LoginVO issueTokens(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername(), user.getRole());
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

    private void recordLoginFailure(String username) {
        String failureKey = loginFailureKey(username);
        long failures = redisUtil.increment(failureKey);
        if (failures == 1) redisUtil.expire(failureKey, 15, TimeUnit.MINUTES);
        if (failures >= MAX_LOGIN_FAILURES) {
            redisUtil.set(loginLockKey(username), "locked", 15, TimeUnit.MINUTES);
            redisUtil.delete(failureKey);
        }
    }

    private String loginFailureKey(String username) {
        return LOGIN_FAILURE_KEY_PREFIX + username;
    }

    private String loginLockKey(String username) {
        return LOGIN_LOCK_KEY_PREFIX + username;
    }
}
