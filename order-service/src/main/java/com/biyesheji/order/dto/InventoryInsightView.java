package com.biyesheji.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public final class InventoryInsightView {

    private InventoryInsightView() {
    }

    public enum Risk {
        OUT_OF_STOCK(1),
        LOW_STOCK(2),
        DEAD_STOCK(3),
        SLOW_MOVING(4),
        OVERSTOCK(5),
        HEALTHY(6);

        private final int priority;

        Risk(int priority) {
            this.priority = priority;
        }

        public int priority() {
            return priority;
        }
    }

    public enum Sort {
        RISK_DESC
    }

    public record Item(
            Long productId,
            String productName,
            Long skuId,
            String skuCode,
            String skuSpecJson,
            Integer productStatus,
            Integer skuStatus,
            int total,
            int locked,
            int available,
            long confirmedQty7d,
            long confirmedQty30d,
            long confirmedQty90d,
            long demandQty30d,
            BigDecimal confirmedRevenue30d,
            BigDecimal dailyVelocity30d,
            BigDecimal daysOfCover,
            LocalDateTime lastConfirmedSaleAt,
            Long daysSinceLastSale,
            long skuAgeDays,
            BigDecimal sellThrough90d,
            Risk risk,
            String ruleCode,
            OffsetDateTime calculatedAt
    ) {
    }

    public record Page(
            List<Item> records,
            long total,
            int pageNum,
            int pageSize,
            long pages
    ) {
        public Page {
            records = List.copyOf(records);
        }
    }

    public record Summary(
            Map<Risk, Long> riskCounts,
            long totalAvailable,
            long confirmedQty30d,
            long noSalesAvailable,
            OffsetDateTime calculatedAt
    ) {
        public Summary {
            riskCounts = Map.copyOf(riskCounts);
        }
    }

    public record ConfirmedSale(
            LocalDateTime paidAt,
            int quantity,
            BigDecimal subtotal
    ) {
    }

    public record Ledger(
            String action,
            int quantity,
            Integer beforeTotal,
            Integer afterTotal,
            Integer beforeLocked,
            Integer afterLocked,
            int beforeAvailable,
            int afterAvailable,
            String referenceNo,
            LocalDateTime createdAt
    ) {
    }

    public record Evidence(
            Item insight,
            List<ConfirmedSale> recentConfirmedSales,
            List<Ledger> recentStockLedgers,
            List<String> limitations
    ) {
        public Evidence {
            recentConfirmedSales = List.copyOf(recentConfirmedSales);
            recentStockLedgers = List.copyOf(recentStockLedgers);
            limitations = List.copyOf(limitations);
        }
    }
}
