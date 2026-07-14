package com.biyesheji.order.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.MD5;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biyesheji.constant.OrderStatus;
import com.biyesheji.constant.ResultCode;
import com.biyesheji.dto.OrderSubmitDTO;
import com.biyesheji.entity.Order;
import com.biyesheji.entity.OrderItem;
import com.biyesheji.entity.Product;
import com.biyesheji.entity.ProductSku;
import com.biyesheji.exception.BizException;
import com.biyesheji.order.mapper.OrderItemMapper;
import com.biyesheji.order.mapper.OrderMapper;
import com.biyesheji.order.mapper.ProductMapper;
import com.biyesheji.order.mapper.ProductSkuMapper;
import com.biyesheji.order.service.OrderService;
import com.biyesheji.order.service.StockService;
import com.biyesheji.utils.RedisUtil;
import com.biyesheji.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Order creation is intentionally synchronous and transactional.  This avoids a
 * "reserved stock but lost message" window that existed when a broker message was
 * published before the order had been persisted.  The stock service reserves stock
 * at submit time; it is confirmed only after payment and released on cancellation.
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final StockService stockService;
    private final RedisUtil redisUtil;

    @Override
    @Transactional
    public String submit(Long userId, OrderSubmitDTO dto) {
        validateDistinctSkus(dto);
        Map<Long, Product> products = loadProducts(dto);
        Map<Long, ProductSku> skus = loadSkus(dto, products);
        BigDecimal totalAmount = calculateTotal(dto, skus);

        String dedupKey = dedupKey(userId, dto);
        if (!redisUtil.setIfAbsent(dedupKey, "processing", 5, TimeUnit.MINUTES)) {
            throw new BizException(ResultCode.ORDER_DUPLICATE, "请勿重复提交订单");
        }

        List<OrderSubmitDTO.OrderItemDTO> reserved = new ArrayList<>();
        boolean created = false;
        try {
            for (OrderSubmitDTO.OrderItemDTO item : dto.getItems()) {
                if (!stockService.deduct(item.getSkuId(), item.getQuantity())) {
                    throw new BizException(ResultCode.STOCK_INSUFFICIENT,
                            "SKU [" + item.getSkuId() + "] 库存不足");
                }
                reserved.add(item);
            }

            String orderNo = IdUtil.getSnowflake().nextIdStr();
            Order order = createOrder(orderNo, userId, totalAmount, dto);
            orderMapper.insert(order);
            for (OrderSubmitDTO.OrderItemDTO item : dto.getItems()) {
                Product product = products.get(item.getProductId());
                ProductSku sku = skus.get(item.getSkuId());
                orderItemMapper.insert(createOrderItem(order, product, sku, item));
            }
            created = true;
            redisUtil.set(dedupKey, orderNo, 5, TimeUnit.MINUTES);
            return orderNo;
        } catch (RuntimeException exception) {
            for (OrderSubmitDTO.OrderItemDTO item : reserved) {
                try {
                    stockService.restore(item.getSkuId(), item.getQuantity());
                } catch (RuntimeException restoreException) {
                    exception.addSuppressed(restoreException);
                }
            }
            throw exception;
        } finally {
            if (!created) {
                redisUtil.delete(dedupKey);
            }
        }
    }

    @Override
    public OrderVO detail(Long userId, String orderNo) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND, "订单不存在");
        }
        return OrderVO.from(order, orderItems(orderNo));
    }

    @Override
    public Page<OrderVO> page(Long userId, int pageNum, int pageSize, Integer status) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .orderByDesc(Order::getCreatedAt);
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        Page<Order> orderPage = orderMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        Page<OrderVO> result = new Page<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
        result.setRecords(orderPage.getRecords().stream()
                .map(order -> OrderVO.from(order, orderItems(order.getOrderNo())))
                .toList());
        return result;
    }

    @Override
    @Transactional
    public void pay(Long userId, String orderNo) {
        Order update = new Order();
        update.setStatus(OrderStatus.PAID.getCode());
        update.setPayTime(LocalDateTime.now());
        int rows = orderMapper.update(update, pendingOrder(userId, orderNo));
        if (rows == 0) {
            throw new BizException("订单不存在或状态不允许支付");
        }
        for (OrderItem item : orderItems(orderNo)) {
            stockService.confirmDeduct(item.getSkuId(), item.getQuantity());
        }
    }

    @Override
    @Transactional
    public void cancel(Long userId, String orderNo) {
        Order update = new Order();
        update.setStatus(OrderStatus.CANCELLED.getCode());
        update.setCancelTime(LocalDateTime.now());
        if (orderMapper.update(update, pendingOrder(userId, orderNo)) == 0) {
            throw new BizException("订单不存在或仅待支付订单可取消");
        }
        restoreOrderItems(orderNo);
    }

    @Override
    @Transactional
    public void processTimeoutOrders() {
        List<Order> timeoutOrders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, OrderStatus.PENDING.getCode())
                .lt(Order::getTimeoutTime, LocalDateTime.now()));
        for (Order order : timeoutOrders) {
            Order update = new Order();
            update.setStatus(OrderStatus.TIMEOUT.getCode());
            update.setCancelTime(LocalDateTime.now());
            LambdaUpdateWrapper<Order> condition = new LambdaUpdateWrapper<Order>()
                    .eq(Order::getOrderNo, order.getOrderNo())
                    .eq(Order::getStatus, OrderStatus.PENDING.getCode());
            if (orderMapper.update(update, condition) == 1) {
                restoreOrderItems(order.getOrderNo());
            }
        }
    }

    private void validateDistinctSkus(OrderSubmitDTO dto) {
        Set<Long> ids = new HashSet<>();
        for (OrderSubmitDTO.OrderItemDTO item : dto.getItems()) {
            if (!ids.add(item.getSkuId())) {
                throw new BizException("同一SKU只能提交一次");
            }
        }
    }

    private Map<Long, Product> loadProducts(OrderSubmitDTO dto) {
        Map<Long, Product> products = new HashMap<>();
        for (OrderSubmitDTO.OrderItemDTO item : dto.getItems()) {
            Product product = productMapper.selectById(item.getProductId());
            if (product == null || product.getStatus() == 0) {
                throw new BizException("商品 [" + item.getProductId() + "] 不存在或已下架");
            }
            products.put(product.getId(), product);
        }
        return products;
    }

    private Map<Long, ProductSku> loadSkus(OrderSubmitDTO dto, Map<Long, Product> products) {
        Map<Long, ProductSku> skus = new HashMap<>();
        for (OrderSubmitDTO.OrderItemDTO item : dto.getItems()) {
            ProductSku sku = productSkuMapper.selectById(item.getSkuId());
            Product product = products.get(item.getProductId());
            if (sku == null || sku.getStatus() != 1 || !item.getProductId().equals(sku.getProductId()) || product == null) {
                throw new BizException("SKU [" + item.getSkuId() + "] 不存在、已停用或不属于商品");
            }
            skus.put(sku.getId(), sku);
        }
        return skus;
    }

    private BigDecimal calculateTotal(OrderSubmitDTO dto, Map<Long, ProductSku> skus) {
        return dto.getItems().stream()
                .map(item -> skus.get(item.getSkuId()).getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String dedupKey(Long userId, OrderSubmitDTO dto) {
        String items = dto.getItems().stream()
                .sorted(Comparator.comparing(OrderSubmitDTO.OrderItemDTO::getSkuId))
                .map(item -> item.getSkuId() + ":" + item.getQuantity())
                .collect(Collectors.joining(","));
        return "order:dedup:" + userId + ":" + MD5.create().digestHex(items);
    }

    private Order createOrder(String orderNo, Long userId, BigDecimal totalAmount, OrderSubmitDTO dto) {
        Order order = new Order();
        order.setId(IdUtil.getSnowflake().nextId());
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.PENDING.getCode());
        order.setReceiverName(dto.getReceiverName());
        order.setReceiverPhone(dto.getReceiverPhone());
        order.setReceiverAddress(dto.getReceiverAddress());
        order.setTimeoutTime(LocalDateTime.now().plusMinutes(30));
        return order;
    }

    private OrderItem createOrderItem(Order order, Product product, ProductSku sku, OrderSubmitDTO.OrderItemDTO item) {
        OrderItem orderItem = new OrderItem();
        orderItem.setId(IdUtil.getSnowflake().nextId());
        orderItem.setOrderId(order.getId());
        orderItem.setOrderNo(order.getOrderNo());
        orderItem.setProductId(product.getId());
        orderItem.setSkuId(sku.getId());
        orderItem.setSkuCode(sku.getSkuCode());
        orderItem.setSkuSpecJson(sku.getSpecJson());
        orderItem.setProductName(product.getName());
        orderItem.setProductImage(product.getMainImage());
        orderItem.setPrice(sku.getPrice());
        orderItem.setQuantity(item.getQuantity());
        orderItem.setSubtotal(sku.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        return orderItem;
    }

    private LambdaUpdateWrapper<Order> pendingOrder(Long userId, String orderNo) {
        return new LambdaUpdateWrapper<Order>()
                .eq(Order::getOrderNo, orderNo)
                .eq(Order::getUserId, userId)
                .eq(Order::getStatus, OrderStatus.PENDING.getCode());
    }

    private List<OrderItem> orderItems(String orderNo) {
        return orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderNo, orderNo));
    }

    private void restoreOrderItems(String orderNo) {
        for (OrderItem item : orderItems(orderNo)) {
            stockService.restore(item.getSkuId(), item.getQuantity());
        }
    }
}
