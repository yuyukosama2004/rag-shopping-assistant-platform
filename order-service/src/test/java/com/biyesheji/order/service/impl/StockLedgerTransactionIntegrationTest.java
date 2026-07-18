package com.biyesheji.order.service.impl;

import com.biyesheji.order.mapper.StockMapper;
import com.biyesheji.order.service.StockService;
import com.biyesheji.utils.RedisUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
        classes = StockLedgerTransactionIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.flyway.enabled=false",
                "mybatis-plus.configuration.map-underscore-to-camel-case=true"
        })
class StockLedgerTransactionIntegrationTest {

    private static final long SKU_ID = 101L;

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.46")
            .withDatabaseName("biyesheji")
            .withUsername("test")
            .withPassword("test-password")
            .withInitScript("sql/stock-ledger-schema.sql");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @Autowired
    private StockService stockService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private RedisUtil redisUtil;

    @BeforeEach
    void resetInventory() {
        jdbcTemplate.update("DELETE FROM t_stock_ledger");
        jdbcTemplate.update("DELETE FROM t_stock");
        jdbcTemplate.execute("ALTER TABLE t_stock_ledger MODIFY action VARCHAR(32) NOT NULL");
        jdbcTemplate.update("""
                INSERT INTO t_stock
                    (id, product_id, sku_id, total, locked, available, version)
                VALUES (1, 1, ?, 10, 0, 10, 0)
                """, SKU_ID);
        reset(redisUtil);
        doReturn(1L).when(redisUtil).executeLua(anyString(), anyList(), any());
    }

    @Test
    void writesCompleteSnapshotsAndKeepsEveryInventoryInvariant() {
        assertTrue(stockService.deduct("ORDER-1", SKU_ID, 2));
        assertTrue(stockService.deduct("ORDER-1", SKU_ID, 2));
        assertStock(10, 2, 8);

        stockService.restore("ORDER-1", SKU_ID, 2);
        stockService.restore("ORDER-1", SKU_ID, 2);
        assertStock(10, 0, 10);

        assertTrue(stockService.deduct("ORDER-2", SKU_ID, 3));
        stockService.confirmDeduct("ORDER-2", SKU_ID, 3);
        stockService.confirmDeduct("ORDER-2", SKU_ID, 3);
        assertStock(7, 0, 7);

        assertEquals(4, ledgerCount());
        assertLedger("ORDER_RESERVE:ORDER-1:101", 10, 10, 0, 2, 10, 8);
        assertLedger("ORDER_RELEASE:ORDER-1:101", 10, 10, 2, 0, 8, 10);
        assertLedger("ORDER_RESERVE:ORDER-2:101", 10, 10, 0, 3, 10, 7);
        assertLedger("ORDER_CONFIRM:ORDER-2:101", 10, 7, 3, 0, 7, 7);
        verify(redisUtil, times(4)).executeLua(anyString(), anyList(), any());
    }

    @Test
    void concurrentDuplicateEventChangesStockAndRedisOnlyOnce() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = executor.submit(() -> {
                start.await();
                return stockService.deduct("ORDER-CONCURRENT", SKU_ID, 2);
            });
            Future<Boolean> second = executor.submit(() -> {
                start.await();
                return stockService.deduct("ORDER-CONCURRENT", SKU_ID, 2);
            });

            start.countDown();
            assertTrue(first.get(10, TimeUnit.SECONDS));
            assertTrue(second.get(10, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }

        assertStock(10, 2, 8);
        assertEquals(1, ledgerCount());
        verify(redisUtil, times(1)).executeLua(anyString(), anyList(), any());
    }

    @Test
    void ledgerInsertFailureRollsBackStockBeforeRedisIsTouched() {
        jdbcTemplate.execute("ALTER TABLE t_stock_ledger MODIFY action VARCHAR(4) NOT NULL");

        assertThrows(RuntimeException.class,
                () -> stockService.deduct("ORDER-ROLLBACK", SKU_ID, 2));

        assertStock(10, 0, 10);
        assertEquals(0, ledgerCount());
        verifyNoInteractions(redisUtil);
    }

    private void assertStock(int total, int locked, int available) {
        Map<String, Object> stock = jdbcTemplate.queryForMap(
                "SELECT total, locked, available FROM t_stock WHERE sku_id = ?", SKU_ID);
        assertEquals(total, ((Number) stock.get("total")).intValue());
        assertEquals(locked, ((Number) stock.get("locked")).intValue());
        assertEquals(available, ((Number) stock.get("available")).intValue());
        assertEquals(total, locked + available);
        assertTrue(total >= locked);
        assertTrue(locked >= 0);
        assertTrue(available >= 0);
    }

    private void assertLedger(String eventKey,
                              int beforeTotal, int afterTotal,
                              int beforeLocked, int afterLocked,
                              int beforeAvailable, int afterAvailable) {
        Map<String, Object> ledger = jdbcTemplate.queryForMap("""
                SELECT before_total, after_total, before_locked, after_locked,
                       before_available, after_available
                FROM t_stock_ledger
                WHERE event_key = ?
                """, eventKey);
        assertEquals(beforeTotal, ((Number) ledger.get("before_total")).intValue());
        assertEquals(afterTotal, ((Number) ledger.get("after_total")).intValue());
        assertEquals(beforeLocked, ((Number) ledger.get("before_locked")).intValue());
        assertEquals(afterLocked, ((Number) ledger.get("after_locked")).intValue());
        assertEquals(beforeAvailable, ((Number) ledger.get("before_available")).intValue());
        assertEquals(afterAvailable, ((Number) ledger.get("after_available")).intValue());
    }

    private int ledgerCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_stock_ledger", Integer.class);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(excludeName = {
            "com.github.xiaoymin.knife4j.spring.configuration.Knife4jAutoConfiguration",
            "org.redisson.spring.starter.RedissonAutoConfigurationV2",
            "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
            "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
    })
    @MapperScan(basePackageClasses = StockMapper.class)
    @Import(StockServiceImpl.class)
    static class TestApplication {
    }
}
