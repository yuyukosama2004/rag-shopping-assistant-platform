package com.biyesheji.order.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.MD5;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biyesheji.constant.OrderStatus;
import com.biyesheji.constant.ResultCode;
import com.biyesheji.dto.OrderSubmitDTO;
import com.biyesheji.entity.*;
import com.biyesheji.exception.BizException;
import com.biyesheji.order.config.RabbitMqConfig;
import com.biyesheji.order.mapper.OrderItemMapper;
import com.biyesheji.order.mapper.OrderMapper;
import com.biyesheji.order.mapper.StockMapper;
import com.biyesheji.order.service.OrderService;
import com.biyesheji.order.service.StockService;
import com.biyesheji.order.mapper.ProductMapper;
import com.biyesheji.utils.RedisUtil;
import com.biyesheji.vo.OrderVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final StockMapper stockMapper;
    private final StockService stockService;
    private final RedisUtil redisUtil;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public String submit(Long userId, OrderSubmitDTO dto) {
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BizException("订单商品不能为空");
        }

        // 1. 幂等校验（基于用户ID + 购物项MD5，5分钟内防重）
        String itemsStr = dto.getItems().stream()
                .sorted(Comparator.comparing(OrderSubmitDTO.OrderItemDTO::getProductId))
                .map(i -> i.getProductId() + ":" + i.getQuantity())
                .collect(Collectors.joining(","));
        String dedupKey = "order:dedup:" + userId + ":" + MD5.create().digestHex(itemsStr);

        if (!redisUtil.setIfAbsent(dedupKey, "1", 5, TimeUnit.MINUTES)) {
            throw new BizException(ResultCode.ORDER_DUPLICATE, "请勿重复提交订单");
        }

        // 2. 校验商品和计算金额
        List<Product> products = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderSubmitDTO.OrderItemDTO item : dto.getItems()) {
            Product product = productMapper.selectById(item.getProductId());
            if (product == null || product.getStatus() == 0) {
                throw new BizException("商品 [" + item.getProductId() + "] 不存在或已下架");
            }
            products.add(product);
            totalAmount = totalAmount.add(product.getPrice().multiply(new BigDecimal(item.getQuantity())));
        }

        // 3. Redis Lua 原子预扣库存
        List<OrderSubmitDTO.OrderItemDTO> items = dto.getItems();
        for (int i = 0; i < items.size(); i++) {
            OrderSubmitDTO.OrderItemDTO item = items.get(i);
            boolean success = stockService.deduct(item.getProductId(), item.getQuantity());
            if (!success) {
                // 回滚已扣的库存（回滚当前索引之前的所有项）
                for (int j = 0; j < i; j++) {
                    stockService.restore(items.get(j).getProductId(), items.get(j).getQuantity());
                }
                throw new BizException(ResultCode.STOCK_INSUFFICIENT,
                        "商品 [" + item.getProductId() + "] 库存不足");
            }
        }

        // 4. 生成订单号
        String orderNo = IdUtil.getSnowflake().nextIdStr();

        // 5. 构建消息，投递 RabbitMQ
        Map<String, Object> orderMsg = new HashMap<>();
        orderMsg.put("orderNo", orderNo);
        orderMsg.put("userId", userId);
        orderMsg.put("totalAmount", totalAmount);
        orderMsg.put("receiverName", dto.getReceiverName());
        orderMsg.put("receiverPhone", dto.getReceiverPhone());
        orderMsg.put("receiverAddress", dto.getReceiverAddress());
        orderMsg.put("items", dto.getItems().stream().map(i -> {
            Map<String, Object> m = new HashMap<>();
            m.put("productId", i.getProductId());
            m.put("quantity", i.getQuantity());
            return m;
        }).collect(Collectors.toList()));
        orderMsg.put("timeoutTime", LocalDateTime.now().plusMinutes(30).toString());

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.ORDER_EXCHANGE,
                    RabbitMqConfig.ORDER_ROUTING_KEY,
                    orderMsg
            );
        } catch (Exception e) {
            log.error("投递订单消息失败，回滚库存", e);
            // 消息投递失败，回滚库存
            for (OrderSubmitDTO.OrderItemDTO item : dto.getItems()) {
                stockService.restore(item.getProductId(), item.getQuantity());
            }
            throw new BizException("下单失败，请稍后重试");
        }

        log.info("订单消息已投递: orderNo={}, userId={}, amount={}", orderNo, userId, totalAmount);
        return orderNo;
    }

    @Override
    @Transactional
    @RabbitListener(queues = RabbitMqConfig.ORDER_QUEUE)
    public void processOrder(Map<String, Object> msg) {
        String orderNo = (String) msg.get("orderNo");
        if (orderNo == null) {
            log.error("MQ消息缺少 orderNo 字段，丢弃: {}", msg);
            return;
        }
        Object uid = msg.get("userId");
        if (uid == null) {
            log.error("MQ消息缺少 userId 字段，丢弃: orderNo={}", orderNo);
            return;
        }
        Long userId = longValue(uid);
        Object amt = msg.get("totalAmount");
        BigDecimal totalAmount = amt != null ? new BigDecimal(amt.toString()) : BigDecimal.ZERO;

        log.info("MQ消费订单: orderNo={}", orderNo);

        // 检查是否已处理（防重复消费）
        Long count = orderMapper.selectCount(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo)
        );
        if (count > 0) {
            log.warn("订单已处理，跳过: orderNo={}", orderNo);
            return;
        }

        // 1. 插入订单
        Order order = new Order();
        order.setId(IdUtil.getSnowflake().nextId());
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.PENDING.getCode());
        order.setReceiverName((String) msg.get("receiverName"));
        order.setReceiverPhone((String) msg.get("receiverPhone"));
        order.setReceiverAddress((String) msg.get("receiverAddress"));
        order.setTimeoutTime(LocalDateTime.now().plusMinutes(30));
        orderMapper.insert(order);

        // 2. 插入订单明细 + 实际扣库存
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) msg.get("items");
        for (Map<String, Object> item : items) {
            Long productId = longValue(item.get("productId"));
            Integer quantity = (Integer) item.get("quantity");

            Product product = productMapper.selectById(productId);

            OrderItem orderItem = new OrderItem();
            orderItem.setId(IdUtil.getSnowflake().nextId());
            orderItem.setOrderId(order.getId());
            orderItem.setOrderNo(orderNo);
            orderItem.setProductId(productId);
            orderItem.setProductName(product != null ? product.getName() : "未知商品");
            orderItem.setProductImage(product != null ? product.getMainImage() : null);
            orderItem.setPrice(product != null ? product.getPrice() : BigDecimal.ZERO);
            orderItem.setQuantity(quantity);
            orderItem.setSubtotal(
                    (product != null ? product.getPrice() : BigDecimal.ZERO)
                            .multiply(new BigDecimal(quantity))
            );
            orderItemMapper.insert(orderItem);

            // 物理扣减 MySQL 库存
            stockService.confirmDeduct(productId, quantity);
        }

        log.info("订单落库成功: orderNo={}, items={}", orderNo, items.size());
    }

    @Override
    public OrderVO detail(Long userId, String orderNo) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo)
        );
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND, "订单不存在");
        }
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderNo, orderNo)
        );
        return OrderVO.from(order, items);
    }

    @Override
    public Page<OrderVO> page(Long userId, int pageNum, int pageSize, Integer status) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId);
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreatedAt);

        Page<Order> orderPage = orderMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<OrderVO> voList = orderPage.getRecords().stream().map(order -> {
            List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderNo, order.getOrderNo())
            );
            return OrderVO.from(order, items);
        }).collect(Collectors.toList());

        Page<OrderVO> voPage = new Page<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    @Transactional
    public void pay(Long userId, String orderNo) {
        // 使用乐观锁条件更新：仅在 status=PENDING 时允许支付
        LambdaUpdateWrapper<Order> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Order::getOrderNo, orderNo)
               .eq(Order::getUserId, userId)
               .eq(Order::getStatus, OrderStatus.PENDING.getCode());
        Order update = new Order();
        update.setStatus(OrderStatus.PAID.getCode());
        update.setPayTime(LocalDateTime.now());
        int rows = orderMapper.update(update, wrapper);
        if (rows == 0) {
            throw new BizException("订单不存在或状态不允许支付");
        }
    }

    @Override
    @Transactional
    public void cancel(Long userId, String orderNo) {
        // 使用乐观锁条件更新：仅在 status=PENDING 时允许取消
        LambdaUpdateWrapper<Order> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Order::getOrderNo, orderNo)
               .eq(Order::getUserId, userId)
               .eq(Order::getStatus, OrderStatus.PENDING.getCode());
        Order update = new Order();
        update.setStatus(OrderStatus.CANCELLED.getCode());
        update.setCancelTime(LocalDateTime.now());
        int rows = orderMapper.update(update, wrapper);
        if (rows == 0) {
            throw new BizException("订单不存在或仅待支付订单可取消");
        }

        // 恢复库存
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderNo, orderNo)
        );
        for (OrderItem item : items) {
            stockService.restore(item.getProductId(), item.getQuantity());
        }
    }

    @Override
    @Transactional
    public void processTimeoutOrders() {
        // 扫描超时未支付订单，使用乐观锁条件更新防止多实例并发重复处理
        List<Order> timeoutOrders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getStatus, OrderStatus.PENDING.getCode())
                        .lt(Order::getTimeoutTime, LocalDateTime.now())
        );

        for (Order order : timeoutOrders) {
            LambdaUpdateWrapper<Order> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(Order::getOrderNo, order.getOrderNo())
                   .eq(Order::getStatus, OrderStatus.PENDING.getCode());
            Order update = new Order();
            update.setStatus(OrderStatus.TIMEOUT.getCode());
            update.setCancelTime(LocalDateTime.now());
            int rows = orderMapper.update(update, wrapper);
            if (rows == 0) {
                continue; // 已被其他实例处理或已被用户支付/取消
            }

            log.info("超时订单自动取消: orderNo={}", order.getOrderNo());

            // 恢复库存
            List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderNo, order.getOrderNo())
            );
            for (OrderItem item : items) {
                stockService.restore(item.getProductId(), item.getQuantity());
            }
        }
    }

    @Override
    @Transactional
    public String createTestOrder(Long userId, Long productId, Integer quantity) {
        Product product = productMapper.selectById(productId);
        if (product == null) throw new BizException("商品不存在");

        // 检查库存
        boolean success = stockService.deduct(productId, quantity);
        if (!success) throw new BizException("库存不足");

        String orderNo = IdUtil.getSnowflake().nextIdStr();
        Order order = new Order();
        order.setId(IdUtil.getSnowflake().nextId());
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setTotalAmount(product.getPrice().multiply(new BigDecimal(quantity)));
        order.setStatus(OrderStatus.PAID.getCode());
        order.setReceiverName("测试用户");
        order.setReceiverPhone("13800000000");
        order.setReceiverAddress("测试地址");
        order.setPayTime(LocalDateTime.now());
        order.setTimeoutTime(LocalDateTime.now().plusMinutes(30));
        orderMapper.insert(order);

        OrderItem orderItem = new OrderItem();
        orderItem.setId(IdUtil.getSnowflake().nextId());
        orderItem.setOrderId(order.getId());
        orderItem.setOrderNo(orderNo);
        orderItem.setProductId(productId);
        orderItem.setProductName(product.getName());
        orderItem.setProductImage(product.getMainImage());
        orderItem.setPrice(product.getPrice());
        orderItem.setQuantity(quantity);
        orderItem.setSubtotal(product.getPrice().multiply(new BigDecimal(quantity)));
        orderItemMapper.insert(orderItem);

        stockService.confirmDeduct(productId, quantity);
        return orderNo;
    }

    private Long longValue(Object obj) {
        if (obj instanceof Integer) {
            return ((Integer) obj).longValue();
        }
        return (Long) obj;
    }
}
