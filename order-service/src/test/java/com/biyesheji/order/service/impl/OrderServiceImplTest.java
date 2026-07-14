package com.biyesheji.order.service.impl;

import com.biyesheji.dto.OrderSubmitDTO;
import com.biyesheji.dto.MerchantShipmentDTO;
import com.biyesheji.entity.OrderItem;
import com.biyesheji.entity.OrderOperation;
import com.biyesheji.entity.Product;
import com.biyesheji.entity.ProductSku;
import com.biyesheji.entity.Order;
import com.biyesheji.entity.ShippingRule;
import com.biyesheji.constant.OrderStatus;
import com.biyesheji.vo.MerchantDashboardVO;
import com.biyesheji.order.mapper.OrderItemMapper;
import com.biyesheji.order.mapper.OrderMapper;
import com.biyesheji.order.mapper.OrderOperationMapper;
import com.biyesheji.order.mapper.ProductMapper;
import com.biyesheji.order.mapper.ProductSkuMapper;
import com.biyesheji.order.service.StockService;
import com.biyesheji.order.service.ShippingRuleService;
import com.biyesheji.utils.RedisUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock private OrderMapper orderMapper;
    @Mock private OrderItemMapper orderItemMapper;
    @Mock private OrderOperationMapper orderOperationMapper;
    @Mock private ProductMapper productMapper;
    @Mock private ProductSkuMapper productSkuMapper;
    @Mock private StockService stockService;
    @Mock private ShippingRuleService shippingRuleService;
    @Mock private RedisUtil redisUtil;
    @InjectMocks private OrderServiceImpl orderService;

    @Test
    void submitUsesSkuPriceAndReservesSkuStock() {
        Product product = new Product();
        product.setId(1L); product.setStatus(1); product.setName("测试商品");
        ProductSku sku = new ProductSku();
        sku.setId(11L); sku.setProductId(1L); sku.setSkuCode("SKU-11"); sku.setSpecJson("{\"颜色\":\"黑色\"}");
        sku.setStatus(1); sku.setPrice(new BigDecimal("199.00"));
        when(productMapper.selectById(1L)).thenReturn(product);
        when(productSkuMapper.selectById(11L)).thenReturn(sku);
        when(redisUtil.setIfAbsent(anyString(), eq("processing"), eq(5L), eq(TimeUnit.MINUTES))).thenReturn(true);
        when(stockService.deduct(11L, 2)).thenReturn(true);
        ShippingRule shippingRule = new ShippingRule();
        shippingRule.setId(1L); shippingRule.setRuleType("DELIVERY"); shippingRule.setName("Standard delivery"); shippingRule.setBaseFee(new BigDecimal("12.00"));
        when(shippingRuleService.requireActive(1L)).thenReturn(shippingRule);
        when(shippingRuleService.calculateFee(shippingRule, new BigDecimal("398.00"))).thenReturn(new BigDecimal("12.00"));

        OrderSubmitDTO dto = new OrderSubmitDTO();
        OrderSubmitDTO.OrderItemDTO item = new OrderSubmitDTO.OrderItemDTO();
        item.setProductId(1L); item.setSkuId(11L); item.setQuantity(2);
        dto.setItems(List.of(item)); dto.setReceiverName("测试用户"); dto.setReceiverPhone("13800000000"); dto.setReceiverAddress("测试地址"); dto.setShippingRuleId(1L);

        String orderNo = orderService.submit(7L, dto);

        assertNotNull(orderNo);
        verify(stockService).deduct(11L, 2);
        ArgumentCaptor<OrderItem> captor = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemMapper).insert(captor.capture());
        assertEquals(11L, captor.getValue().getSkuId());
        assertEquals("SKU-11", captor.getValue().getSkuCode());
        assertEquals(new BigDecimal("199.00"), captor.getValue().getPrice());
        assertEquals(new BigDecimal("398.00"), captor.getValue().getSubtotal());
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderMapper).insert(orderCaptor.capture());
        assertEquals(new BigDecimal("398.00"), orderCaptor.getValue().getProductAmount());
        assertEquals(new BigDecimal("12.00"), orderCaptor.getValue().getShippingFee());
        assertEquals(new BigDecimal("410.00"), orderCaptor.getValue().getTotalAmount());
    }

    @Test
    void shipRecordsMerchantOperationAfterPaidTransition() {
        when(orderMapper.update(any(), any())).thenReturn(1);
        MerchantShipmentDTO dto = new MerchantShipmentDTO();
        dto.setCarrier("顺丰"); dto.setTrackingNo("SF123"); dto.setNote("已核验包装");

        orderService.ship(9L, "ORDER-1", dto);

        ArgumentCaptor<OrderOperation> captor = ArgumentCaptor.forClass(OrderOperation.class);
        verify(orderOperationMapper).insert(captor.capture());
        assertEquals("ORDER-1", captor.getValue().getOrderNo());
        assertEquals("MERCHANT_SHIP", captor.getValue().getAction());
        assertEquals(9L, captor.getValue().getOperatorId());
    }

    @Test
    void acceptsCodOrderWithoutPretendingItWasPaid() {
        Order order = new Order();
        order.setOrderNo("ORDER-COD"); order.setPaymentMethod("COD"); order.setStatus(OrderStatus.PENDING.getCode());
        when(orderMapper.selectOne(any())).thenReturn(order);
        when(orderMapper.update(any(), any())).thenReturn(1);

        orderService.accept(9L, "ORDER-COD");

        ArgumentCaptor<OrderOperation> captor = ArgumentCaptor.forClass(OrderOperation.class);
        verify(orderOperationMapper).insert(captor.capture());
        assertEquals("MERCHANT_ACCEPT", captor.getValue().getAction());
    }

    @Test
    void dashboardCountsOnlyConfirmedSales() {
        Order confirmed = new Order(); confirmed.setTotalAmount(new BigDecimal("120.00")); confirmed.setPayTime(LocalDateTime.now());
        Order unconfirmed = new Order(); unconfirmed.setTotalAmount(new BigDecimal("80.00"));
        when(orderMapper.selectList(any())).thenReturn(List.of(confirmed, unconfirmed));
        when(orderMapper.selectCount(any())).thenReturn(3L);

        MerchantDashboardVO dashboard = orderService.merchantDashboard();

        assertEquals(2, dashboard.getTodayOrderCount());
        assertEquals(3, dashboard.getPendingOrderCount());
        assertEquals(new BigDecimal("120.00"), dashboard.getTodayConfirmedSales());
    }
}
