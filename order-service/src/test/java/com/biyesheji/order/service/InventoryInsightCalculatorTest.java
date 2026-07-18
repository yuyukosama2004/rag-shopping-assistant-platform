package com.biyesheji.order.service;

import com.biyesheji.order.config.InventoryInsightProperties;
import com.biyesheji.order.dto.InventoryInsightRow;
import com.biyesheji.order.dto.InventoryInsightView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class InventoryInsightCalculatorTest {

    private static final OffsetDateTime CALCULATED_AT =
            OffsetDateTime.parse("2026-07-18T12:00:00+08:00");

    private final InventoryInsightCalculator calculator = new InventoryInsightCalculator();
    private InventoryInsightProperties properties;

    @BeforeEach
    void setUp() {
        properties = new InventoryInsightProperties();
    }

    @Test
    void calculatesPublishedMetricExampleWithoutRoundingDrift() {
        InventoryInsightRow row = row(1L, 42, 2, 4, 180);
        row.setConfirmedQty7d(0L);
        row.setDemandQty30d(3L);
        row.setConfirmedRevenue30d(new BigDecimal("5998.00"));
        row.setLastConfirmedSaleAt(CALCULATED_AT.minusDays(17).toLocalDateTime());

        InventoryInsightView.Item item = calculator.calculate(row, CALCULATED_AT, properties);

        assertEquals(new BigDecimal("0.0667"), item.dailyVelocity30d());
        assertEquals(new BigDecimal("630.0"), item.daysOfCover());
        assertEquals(new BigDecimal("0.0870"), item.sellThrough90d());
        assertEquals(17L, item.daysSinceLastSale());
        assertEquals(180L, item.skuAgeDays());
        assertEquals(InventoryInsightView.Risk.OVERSTOCK, item.risk());
    }

    @Test
    void appliesRiskRulesInPublishedPriorityOrder() {
        assertRisk(row(1L, 0, 0, 0, 100), InventoryInsightView.Risk.OUT_OF_STOCK);
        assertRisk(row(2L, 5, 1, 1, 100), InventoryInsightView.Risk.LOW_STOCK);
        assertRisk(row(3L, 7, 0, 0, 30), InventoryInsightView.Risk.DEAD_STOCK);
        assertRisk(row(4L, 8, 0, 1, 100), InventoryInsightView.Risk.SLOW_MOVING);
        assertRisk(row(5L, 100, 10, 10, 100), InventoryInsightView.Risk.OVERSTOCK);
        assertRisk(row(6L, 8, 4, 4, 100), InventoryInsightView.Risk.HEALTHY);

        InventoryInsightRow inactiveEmpty = row(7L, 0, 0, 0, 10);
        inactiveEmpty.setProductStatus(0);
        assertRisk(inactiveEmpty, InventoryInsightView.Risk.HEALTHY);
    }

    @Test
    void returnsNullCoverageAndSellThroughWhenTheirDenominatorsAreZero() {
        InventoryInsightView.Item item = calculator.calculate(
                row(1L, 0, 0, 0, 10),
                CALCULATED_AT,
                properties
        );

        assertNull(item.daysOfCover());
        assertNull(item.sellThrough90d());
    }

    private void assertRisk(InventoryInsightRow row, InventoryInsightView.Risk expected) {
        assertEquals(expected, calculator.calculate(row, CALCULATED_AT, properties).risk());
    }

    private InventoryInsightRow row(
            long skuId,
            int available,
            long confirmed30d,
            long confirmed90d,
            long ageDays
    ) {
        InventoryInsightRow row = new InventoryInsightRow();
        row.setProductId(skuId);
        row.setProductName("Product " + skuId);
        row.setSkuId(skuId);
        row.setSkuCode("SKU-" + skuId);
        row.setSkuSpecJson("{}");
        row.setProductStatus(1);
        row.setSkuStatus(1);
        row.setTotal(available);
        row.setLocked(0);
        row.setAvailable(available);
        row.setConfirmedQty7d(confirmed30d);
        row.setConfirmedQty30d(confirmed30d);
        row.setConfirmedQty90d(confirmed90d);
        row.setDemandQty30d(confirmed30d);
        row.setConfirmedRevenue30d(BigDecimal.ZERO);
        row.setSkuCreatedAt(CALCULATED_AT.minusDays(ageDays).toLocalDateTime());
        return row;
    }
}
