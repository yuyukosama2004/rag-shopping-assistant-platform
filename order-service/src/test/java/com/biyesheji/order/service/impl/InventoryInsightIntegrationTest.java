package com.biyesheji.order.service.impl;

import com.biyesheji.order.config.InventoryInsightProperties;
import com.biyesheji.order.dto.InventoryInsightView;
import com.biyesheji.order.mapper.InventoryInsightMapper;
import com.biyesheji.order.service.InventoryInsightCalculator;
import com.biyesheji.order.service.InventoryInsightService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
        classes = InventoryInsightIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.flyway.enabled=false",
                "mybatis-plus.configuration.map-underscore-to-camel-case=true"
        })
class InventoryInsightIntegrationTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-07-18T04:00:00Z"),
            ZONE
    );

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.46")
            .withDatabaseName("biyesheji")
            .withUsername("test")
            .withPassword("test-password")
            .withInitScript("sql/inventory-insight-schema.sql");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @Autowired
    private InventoryInsightMapper mapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private InventoryInsightService service;

    @BeforeEach
    void seedFixtures() {
        jdbcTemplate.update("DELETE FROM t_stock_ledger");
        jdbcTemplate.update("DELETE FROM t_order_item");
        jdbcTemplate.update("DELETE FROM t_order");
        jdbcTemplate.update("DELETE FROM t_stock");
        jdbcTemplate.update("DELETE FROM t_product_sku");
        jdbcTemplate.update("DELETE FROM t_product");

        for (long id = 1; id <= 6; id++) {
            jdbcTemplate.update("""
                    INSERT INTO t_product
                        (id, name, status, created_at, updated_at, deleted)
                    VALUES (?, ?, 1, '2025-01-01 00:00:00', '2025-01-01 00:00:00', 0)
                    """, id, productName(id));
            jdbcTemplate.update("""
                    INSERT INTO t_product_sku
                        (id, product_id, sku_code, spec_json, price, status, created_at, updated_at)
                    VALUES (?, ?, ?, '{}', 10.01, 1, ?, ?)
                    """,
                    100 + id,
                    id,
                    "SKU-" + id,
                    skuCreatedAt(id),
                    skuCreatedAt(id));
        }

        insertStock(101, 0);
        insertStock(102, 5);
        insertStock(103, 7);
        insertStock(104, 8);
        insertStock(105, 100);
        insertStock(106, 8);

        insertOrder(201, 102, 2, "199.98", 1,
                "2026-07-16 12:00:00", "2026-07-16 12:00:00", "OFFLINE");
        insertOrder(202, 104, 1, "50.00", 3,
                "2026-05-19 12:00:00", "2026-05-19 12:00:00", "OFFLINE");
        insertOrder(203, 105, 10, "1000.10", 1,
                "2026-07-08 12:00:00", "2026-07-08 12:00:00", "OFFLINE");
        insertOrder(204, 106, 3, "30.03", 1,
                "2026-07-11 12:00:00", "2026-07-11 12:00:00", "OFFLINE");
        insertOrder(205, 106, 4, "40.04", 2,
                null, "2026-07-17 12:00:00", "COD");
        insertOrder(206, 106, 50, "500.50", 4,
                null, "2026-07-17 12:10:00", "OFFLINE");
        insertOrder(207, 106, 60, "600.60", 5,
                null, "2026-07-17 12:20:00", "OFFLINE");
        insertOrder(208, 106, 1, "10.01", 4,
                "2026-07-17 12:30:00", "2026-07-17 12:30:00", "OFFLINE");

        jdbcTemplate.update("""
                INSERT INTO t_stock_ledger
                    (id, sku_id, action, event_key, quantity,
                     before_total, after_total, before_locked, after_locked,
                     before_available, after_available, reference_no, created_at)
                VALUES
                    (1, 106, 'ORDER_RESERVE', 'ORDER_RESERVE:ORDER-204:106', 3,
                     8, 8, 0, 3, 8, 5, 'ORDER-204', '2026-07-11 11:59:00')
                """);

        InventoryInsightProperties properties = new InventoryInsightProperties();
        service = new InventoryInsightServiceImpl(
                mapper,
                new InventoryInsightCalculator(),
                properties,
                CLOCK
        );
    }

    @Test
    void fixtureMatchesManualRiskOrderAndMetricWindows() {
        InventoryInsightView.Page page = service.list(
                1,
                20,
                null,
                null,
                InventoryInsightView.Sort.RISK_DESC
        );

        assertEquals(
                List.of(
                        InventoryInsightView.Risk.OUT_OF_STOCK,
                        InventoryInsightView.Risk.LOW_STOCK,
                        InventoryInsightView.Risk.DEAD_STOCK,
                        InventoryInsightView.Risk.SLOW_MOVING,
                        InventoryInsightView.Risk.OVERSTOCK,
                        InventoryInsightView.Risk.HEALTHY
                ),
                page.records().stream().map(InventoryInsightView.Item::risk).toList()
        );

        InventoryInsightView.Item healthy = bySku(page.records(), 106L);
        assertEquals(4, healthy.confirmedQty7d());
        assertEquals(4, healthy.confirmedQty30d());
        assertEquals(4, healthy.confirmedQty90d());
        assertEquals(7, healthy.demandQty30d());
        assertEquals(new BigDecimal("40.04"), healthy.confirmedRevenue30d());
        assertEquals(new BigDecimal("0.3333"), healthy.sellThrough90d());
    }

    @Test
    void supportsStableFilteringPaginationSummaryAndEvidenceWithoutAi() throws Exception {
        InventoryInsightView.Page first = service.list(
                1,
                2,
                null,
                null,
                InventoryInsightView.Sort.RISK_DESC
        );
        InventoryInsightView.Page repeated = service.list(
                1,
                2,
                null,
                null,
                InventoryInsightView.Sort.RISK_DESC
        );
        InventoryInsightView.Page filtered = service.list(
                1,
                20,
                "Over",
                InventoryInsightView.Risk.OVERSTOCK,
                InventoryInsightView.Sort.RISK_DESC
        );

        assertEquals(first, repeated);
        assertEquals(6, first.total());
        assertEquals(3, first.pages());
        assertEquals(List.of(101L, 102L),
                first.records().stream().map(InventoryInsightView.Item::skuId).toList());
        assertEquals(List.of(105L),
                filtered.records().stream().map(InventoryInsightView.Item::skuId).toList());

        InventoryInsightView.Summary summary = service.summary();
        assertEquals(128, summary.totalAvailable());
        assertEquals(16, summary.confirmedQty30d());
        assertEquals(15, summary.noSalesAvailable());
        for (InventoryInsightView.Risk risk : InventoryInsightView.Risk.values()) {
            assertEquals(1, summary.riskCounts().get(risk));
        }

        InventoryInsightView.Evidence evidence = service.evidence(106L);
        assertEquals(2, evidence.recentConfirmedSales().size());
        assertEquals(1, evidence.recentStockLedgers().size());
        assertEquals("ORDER-204", evidence.recentStockLedgers().get(0).referenceNo());
        assertFalse(evidence.limitations().isEmpty());

        String evidenceJson = new ObjectMapper().findAndRegisterModules()
                .writeValueAsString(evidence);
        assertFalse(evidenceJson.contains("Private Name"));
        assertFalse(evidenceJson.contains("13800000000"));
        assertFalse(evidenceJson.contains("Private Address"));
        assertFalse(evidenceJson.contains("Private Note"));
    }

    @Test
    void explainShowsExistingSkuAndOrderJoinIndexesAreAvailable() {
        List<Map<String, Object>> plan = jdbcTemplate.queryForList("""
                EXPLAIN
                SELECT sku.id,
                       SUM(CASE WHEN o.pay_time IS NOT NULL THEN oi.quantity ELSE 0 END)
                FROM t_product_sku sku
                LEFT JOIN t_order_item oi ON oi.sku_id = sku.id
                LEFT JOIN t_order o ON o.id = oi.order_id AND o.deleted = 0
                GROUP BY sku.id
                """);

        Map<String, Object> orderItemStep = step(plan, "oi");
        Map<String, Object> orderStep = step(plan, "o");
        assertTrue(String.valueOf(orderItemStep.get("possible_keys"))
                .contains("idx_order_item_sku"));
        assertTrue(
                String.valueOf(orderStep.get("possible_keys")).contains("PRIMARY")
                        || "eq_ref".equals(orderStep.get("type"))
        );
        assertNotNull(orderStep);
    }

    private InventoryInsightView.Item bySku(List<InventoryInsightView.Item> items, Long skuId) {
        return items.stream()
                .filter(item -> skuId.equals(item.skuId()))
                .findFirst()
                .orElseThrow();
    }

    private Map<String, Object> step(List<Map<String, Object>> plan, String table) {
        return plan.stream()
                .filter(row -> table.equals(row.get("table")))
                .findFirst()
                .orElseThrow();
    }

    private void insertStock(long skuId, int available) {
        jdbcTemplate.update("""
                INSERT INTO t_stock
                    (product_id, sku_id, total, locked, available, version)
                VALUES (?, ?, ?, 0, ?, 0)
                """, skuId - 100, skuId, available, available);
    }

    private void insertOrder(
            long orderId,
            long skuId,
            int quantity,
            String subtotal,
            int status,
            String payTime,
            String createdAt,
            String paymentMethod
    ) {
        String orderNo = "ORDER-" + orderId;
        jdbcTemplate.update("""
                INSERT INTO t_order
                    (id, order_no, user_id, total_amount, payment_method, status,
                     receiver_name, receiver_phone, receiver_address, merchant_note,
                     pay_time, created_at, updated_at, deleted)
                VALUES (?, ?, 999, ?, ?, ?, 'Private Name', '13800000000',
                        'Private Address', 'Private Note', ?, ?, ?, 0)
                """,
                orderId,
                orderNo,
                new BigDecimal(subtotal),
                paymentMethod,
                status,
                payTime,
                createdAt,
                createdAt);
        jdbcTemplate.update("""
                INSERT INTO t_order_item
                    (id, order_id, order_no, product_id, sku_id, sku_code,
                     product_name, price, quantity, subtotal, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, 10.01, ?, ?, ?)
                """,
                orderId,
                orderId,
                orderNo,
                skuId - 100,
                skuId,
                "SKU-" + (skuId - 100),
                productName(skuId - 100),
                quantity,
                new BigDecimal(subtotal),
                createdAt);
    }

    private String productName(long id) {
        return switch ((int) id) {
            case 1 -> "Out Product";
            case 2 -> "Low Product";
            case 3 -> "Dead Product";
            case 4 -> "Slow Product";
            case 5 -> "Over Product";
            case 6 -> "Healthy Product";
            default -> throw new IllegalArgumentException("Unknown fixture product");
        };
    }

    private String skuCreatedAt(long id) {
        return switch ((int) id) {
            case 1, 2, 4, 5, 6 -> "2026-01-01 00:00:00";
            case 3 -> "2026-06-08 12:00:00";
            default -> throw new IllegalArgumentException("Unknown fixture SKU");
        };
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(excludeName = {
            "com.github.xiaoymin.knife4j.spring.configuration.Knife4jAutoConfiguration",
            "org.redisson.spring.starter.RedissonAutoConfigurationV2",
            "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
            "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
    })
    @MapperScan(basePackageClasses = InventoryInsightMapper.class)
    static class TestApplication {
    }
}
