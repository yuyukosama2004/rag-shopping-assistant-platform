package com.biyesheji.order.service.impl;

import com.biyesheji.exception.BizException;
import com.biyesheji.order.config.InventoryInsightProperties;
import com.biyesheji.order.dto.InventoryInsightRow;
import com.biyesheji.order.dto.InventoryInsightView;
import com.biyesheji.order.dto.InventoryLedgerEvidenceRow;
import com.biyesheji.order.dto.InventorySaleEvidenceRow;
import com.biyesheji.order.mapper.InventoryInsightMapper;
import com.biyesheji.order.service.InventoryInsightCalculator;
import com.biyesheji.order.service.InventoryInsightService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class InventoryInsightServiceImpl implements InventoryInsightService {

    private static final int EVIDENCE_LIMIT = 10;
    private static final List<String> LIMITATIONS = List.of(
            "确认销量以订单 pay_time 非空为准，当前未按退款明细数量回冲。",
            "当前没有采购批次和采购成本，不计算利润、资金占用或精确库龄。",
            "所有结果均为只读经营事实，不会自动修改价格、库存或商品状态。"
    );

    private final InventoryInsightMapper mapper;
    private final InventoryInsightCalculator calculator;
    private final InventoryInsightProperties properties;
    private final Clock clock;

    @Autowired
    public InventoryInsightServiceImpl(
            InventoryInsightMapper mapper,
            InventoryInsightCalculator calculator,
            InventoryInsightProperties properties
    ) {
        this(
                mapper,
                calculator,
                properties,
                Clock.system(ZoneId.of(properties.getTimeZone()))
        );
    }

    public InventoryInsightServiceImpl(
            InventoryInsightMapper mapper,
            InventoryInsightCalculator calculator,
            InventoryInsightProperties properties,
            Clock clock
    ) {
        this.mapper = mapper;
        this.calculator = calculator;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public InventoryInsightView.Page list(
            int pageNum,
            int pageSize,
            String keyword,
            InventoryInsightView.Risk risk,
            InventoryInsightView.Sort sort
    ) {
        if (sort != InventoryInsightView.Sort.RISK_DESC) {
            throw new BizException(400, "Unsupported inventory insight sort");
        }
        SnapshotTime time = snapshotTime();
        List<InventoryInsightView.Item> items = calculateAll(time, normalizeKeyword(keyword));
        if (risk != null) {
            items = items.stream().filter(item -> item.risk() == risk).toList();
        }
        items = items.stream().sorted(riskOrder()).toList();

        long total = items.size();
        long offset = (long) (pageNum - 1) * pageSize;
        List<InventoryInsightView.Item> records;
        if (offset >= total) {
            records = List.of();
        } else {
            int from = Math.toIntExact(offset);
            int to = Math.min(from + pageSize, items.size());
            records = items.subList(from, to);
        }
        long pages = total == 0 ? 0 : (total + pageSize - 1) / pageSize;
        return new InventoryInsightView.Page(records, total, pageNum, pageSize, pages);
    }

    @Override
    public InventoryInsightView.Summary summary() {
        SnapshotTime time = snapshotTime();
        List<InventoryInsightView.Item> items = calculateAll(time, null);
        Map<InventoryInsightView.Risk, Long> riskCounts = new LinkedHashMap<>();
        for (InventoryInsightView.Risk risk : InventoryInsightView.Risk.values()) {
            riskCounts.put(risk, 0L);
        }
        long totalAvailable = 0;
        long confirmedQty30d = 0;
        long noSalesAvailable = 0;
        for (InventoryInsightView.Item item : items) {
            riskCounts.compute(item.risk(), (ignored, count) -> count + 1);
            totalAvailable += item.available();
            confirmedQty30d += item.confirmedQty30d();
            if (item.confirmedQty30d() == 0) {
                noSalesAvailable += item.available();
            }
        }
        return new InventoryInsightView.Summary(
                riskCounts,
                totalAvailable,
                confirmedQty30d,
                noSalesAvailable,
                time.calculatedAt()
        );
    }

    @Override
    public InventoryInsightView.Evidence evidence(Long skuId) {
        SnapshotTime time = snapshotTime();
        InventoryInsightRow row = mapper.selectSnapshotBySkuId(
                skuId,
                time.asOf(),
                time.start7(),
                time.start30(),
                time.start90()
        );
        if (row == null) {
            throw new BizException(404, "Inventory insight SKU not found");
        }
        InventoryInsightView.Item item = calculator.calculate(row, time.calculatedAt(), properties);
        List<InventoryInsightView.ConfirmedSale> sales = mapper
                .selectRecentConfirmedSales(skuId, time.asOf(), EVIDENCE_LIMIT)
                .stream()
                .map(this::toSale)
                .toList();
        List<InventoryInsightView.Ledger> ledgers = mapper
                .selectRecentStockLedgers(skuId, EVIDENCE_LIMIT)
                .stream()
                .map(this::toLedger)
                .toList();
        return new InventoryInsightView.Evidence(item, sales, ledgers, LIMITATIONS);
    }

    private List<InventoryInsightView.Item> calculateAll(SnapshotTime time, String keyword) {
        List<InventoryInsightView.Item> items = new ArrayList<>();
        for (InventoryInsightRow row : mapper.selectSnapshots(
                time.asOf(),
                time.start7(),
                time.start30(),
                time.start90(),
                keyword
        )) {
            items.add(calculator.calculate(row, time.calculatedAt(), properties));
        }
        return items;
    }

    private SnapshotTime snapshotTime() {
        OffsetDateTime calculatedAt = OffsetDateTime.now(clock);
        LocalDateTime asOf = calculatedAt.toLocalDateTime();
        return new SnapshotTime(
                calculatedAt,
                asOf,
                asOf.minusDays(7),
                asOf.minusDays(30),
                asOf.minusDays(properties.getDeadStockWindowDays())
        );
    }

    private Comparator<InventoryInsightView.Item> riskOrder() {
        return Comparator.comparingInt((InventoryInsightView.Item item) -> item.risk().priority())
                .thenComparing(InventoryInsightView.Item::productId)
                .thenComparing(InventoryInsightView.Item::skuId);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String normalized = keyword.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private InventoryInsightView.ConfirmedSale toSale(InventorySaleEvidenceRow row) {
        return new InventoryInsightView.ConfirmedSale(
                row.getPaidAt(),
                row.getQuantity(),
                row.getSubtotal()
        );
    }

    private InventoryInsightView.Ledger toLedger(InventoryLedgerEvidenceRow row) {
        return new InventoryInsightView.Ledger(
                row.getAction(),
                row.getQuantity(),
                row.getBeforeTotal(),
                row.getAfterTotal(),
                row.getBeforeLocked(),
                row.getAfterLocked(),
                row.getBeforeAvailable(),
                row.getAfterAvailable(),
                row.getReferenceNo(),
                row.getCreatedAt()
        );
    }

    private record SnapshotTime(
            OffsetDateTime calculatedAt,
            LocalDateTime asOf,
            LocalDateTime start7,
            LocalDateTime start30,
            LocalDateTime start90
    ) {
    }
}
