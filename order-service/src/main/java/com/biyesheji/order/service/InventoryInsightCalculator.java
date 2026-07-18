package com.biyesheji.order.service;

import com.biyesheji.order.config.InventoryInsightProperties;
import com.biyesheji.order.dto.InventoryInsightRow;
import com.biyesheji.order.dto.InventoryInsightView;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

@Component
public class InventoryInsightCalculator {

    private static final BigDecimal THIRTY_DAYS = BigDecimal.valueOf(30);

    public InventoryInsightView.Item calculate(
            InventoryInsightRow row,
            OffsetDateTime calculatedAt,
            InventoryInsightProperties properties
    ) {
        int total = value(row.getTotal());
        int locked = value(row.getLocked());
        int available = value(row.getAvailable());
        long confirmed7d = value(row.getConfirmedQty7d());
        long confirmed30d = value(row.getConfirmedQty30d());
        long confirmed90d = value(row.getConfirmedQty90d());
        long demand30d = value(row.getDemandQty30d());
        BigDecimal revenue30d = money(row.getConfirmedRevenue30d());

        BigDecimal dailyVelocity = BigDecimal.valueOf(confirmed30d)
                .divide(THIRTY_DAYS, 4, RoundingMode.HALF_UP);
        BigDecimal daysOfCover = confirmed30d == 0
                ? null
                : BigDecimal.valueOf(available)
                        .multiply(THIRTY_DAYS)
                        .divide(BigDecimal.valueOf(confirmed30d), 1, RoundingMode.HALF_UP);
        long sellThroughDenominator = confirmed90d + available;
        BigDecimal sellThrough90d = sellThroughDenominator == 0
                ? null
                : BigDecimal.valueOf(confirmed90d)
                        .divide(BigDecimal.valueOf(sellThroughDenominator), 4, RoundingMode.HALF_UP);

        LocalDateTime localCalculatedAt = calculatedAt.toLocalDateTime();
        Long daysSinceLastSale = row.getLastConfirmedSaleAt() == null
                ? null
                : nonNegativeDays(row.getLastConfirmedSaleAt(), localCalculatedAt);
        long skuAgeDays = nonNegativeDays(row.getSkuCreatedAt(), localCalculatedAt);
        Classification classification = classify(
                row,
                available,
                confirmed30d,
                confirmed90d,
                skuAgeDays,
                daysOfCover,
                properties
        );

        return new InventoryInsightView.Item(
                row.getProductId(),
                row.getProductName(),
                row.getSkuId(),
                row.getSkuCode(),
                row.getSkuSpecJson(),
                row.getProductStatus(),
                row.getSkuStatus(),
                total,
                locked,
                available,
                confirmed7d,
                confirmed30d,
                confirmed90d,
                demand30d,
                revenue30d,
                dailyVelocity,
                daysOfCover,
                row.getLastConfirmedSaleAt(),
                daysSinceLastSale,
                skuAgeDays,
                sellThrough90d,
                classification.risk(),
                classification.ruleCode(),
                calculatedAt
        );
    }

    private Classification classify(
            InventoryInsightRow row,
            int available,
            long confirmed30d,
            long confirmed90d,
            long skuAgeDays,
            BigDecimal daysOfCover,
            InventoryInsightProperties properties
    ) {
        boolean selling = Integer.valueOf(1).equals(row.getProductStatus())
                && Integer.valueOf(1).equals(row.getSkuStatus());
        if (selling && available == 0) {
            return new Classification(
                    InventoryInsightView.Risk.OUT_OF_STOCK,
                    "SELLING_AND_AVAILABLE_EQ_0"
            );
        }
        if (selling
                && available > 0
                && available <= properties.getLowStockThreshold()
                && confirmed30d > 0) {
            return new Classification(
                    InventoryInsightView.Risk.LOW_STOCK,
                    "SELLING_LOW_AVAILABLE_WITH_30D_SALES"
            );
        }
        if (available > 0
                && skuAgeDays >= properties.getDeadStockMinAgeDays()
                && confirmed90d == 0) {
            return new Classification(
                    InventoryInsightView.Risk.DEAD_STOCK,
                    "AVAILABLE_WITH_MIN_AGE_AND_NO_90D_SALES"
            );
        }
        if (available > 0 && confirmed30d == 0 && confirmed90d > 0) {
            return new Classification(
                    InventoryInsightView.Risk.SLOW_MOVING,
                    "AVAILABLE_WITH_NO_30D_SALES_AND_POSITIVE_90D_SALES"
            );
        }
        if (available >= properties.getOverstockMinAvailable()
                && daysOfCover != null
                && daysOfCover.compareTo(BigDecimal.valueOf(properties.getOverstockCoverDays())) > 0) {
            return new Classification(
                    InventoryInsightView.Risk.OVERSTOCK,
                    "AVAILABLE_GTE_MIN_AND_COVER_GT_LIMIT"
            );
        }
        return new Classification(InventoryInsightView.Risk.HEALTHY, "NO_RISK_RULE_MATCHED");
    }

    private long nonNegativeDays(LocalDateTime from, LocalDateTime to) {
        if (from == null) {
            return 0;
        }
        return Math.max(0, ChronoUnit.DAYS.between(from.toLocalDate(), to.toLocalDate()));
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private long value(Long value) {
        return value == null ? 0 : value;
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private record Classification(InventoryInsightView.Risk risk, String ruleCode) {
    }
}
