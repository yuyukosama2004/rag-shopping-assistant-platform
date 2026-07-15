package com.biyesheji.order.service;

import com.biyesheji.entity.AiRequestLog;
import com.biyesheji.entity.AiSetting;
import com.biyesheji.exception.BizException;
import com.biyesheji.order.mapper.AiRequestLogMapper;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiUsageService {
    private static final BigDecimal ONE_MILLION = new BigDecimal("1000000");
    private final AiRequestLogMapper mapper;
    private final MeterRegistry meterRegistry;
    private final AtomicReference<Double> dailyEstimatedCost = new AtomicReference<>(0D);
    private final AtomicReference<Double> dailyBudget = new AtomicReference<>(0D);

    @PostConstruct
    void registerMetrics() {
        Gauge.builder("biyesheji.ai.daily.estimated.cost", dailyEstimatedCost, value -> value.get())
                .description("Estimated AI cost recorded for the current day")
                .register(meterRegistry);
        Gauge.builder("biyesheji.ai.daily.budget", dailyBudget, value -> value.get())
                .description("Merchant-configured daily AI budget")
                .register(meterRegistry);
    }

    public void requireBudget(AiSetting setting) {
        BigDecimal used = mapper.todayCost();
        updateDailyMetrics(setting, used);
        if (setting.getDailyBudget().signum() == 0) return;
        if (used != null && used.compareTo(setting.getDailyBudget()) >= 0) {
            throw new BizException(429, "今日 AI 预算已用完，请联系商家或明天再试");
        }
    }

    public void recordSuccess(AiSetting setting, int inputChars, int outputChars, long durationMs) {
        record(setting, "SUCCESS", inputChars, outputChars, durationMs, null);
    }

    public void recordFailure(AiSetting setting, int inputChars, long durationMs, String reason) {
        record(setting, "FAILED", inputChars, 0, durationMs, reason);
    }

    public Map<String, Object> todaySummary(AiSetting setting) {
        Map<String, Object> result = new LinkedHashMap<>(mapper.todaySummary());
        result.put("dailyBudget", setting.getDailyBudget());
        updateDailyMetrics(setting, mapper.todayCost());
        return result;
    }

    private void record(AiSetting setting, String status, int inputChars, int outputChars,
                        long durationMs, String reason) {
        try {
            int inputTokens = estimateTokens(inputChars);
            int outputTokens = estimateTokens(outputChars);
            BigDecimal cost = setting.getInputPricePerMillion().multiply(BigDecimal.valueOf(inputTokens))
                    .add(setting.getOutputPricePerMillion().multiply(BigDecimal.valueOf(outputTokens)))
                    .divide(ONE_MILLION, 6, RoundingMode.HALF_UP);
            AiRequestLog requestLog = new AiRequestLog();
            requestLog.setModel(setting.getModel());
            requestLog.setStatus(status);
            requestLog.setInputChars(inputChars);
            requestLog.setOutputChars(outputChars);
            requestLog.setEstimatedInputTokens(inputTokens);
            requestLog.setEstimatedOutputTokens(outputTokens);
            requestLog.setEstimatedCost(cost);
            requestLog.setDurationMs(durationMs);
            requestLog.setFailureReason(trimReason(reason));
            mapper.insert(requestLog);
            dailyEstimatedCost.updateAndGet(value -> value + cost.doubleValue());
            meterRegistry.counter("biyesheji.ai.requests", "outcome", status.toLowerCase()).increment();
            meterRegistry.counter("biyesheji.ai.estimated.cost").increment(cost.doubleValue());
            meterRegistry.timer("biyesheji.ai.request.duration", "outcome", status.toLowerCase())
                    .record(durationMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.warn("记录 AI 请求指标失败: {}", e.getMessage());
        }
    }

    int estimateTokens(int chars) {
        return chars <= 0 ? 0 : (chars + 1) / 2;
    }

    private void updateDailyMetrics(AiSetting setting, BigDecimal used) {
        dailyBudget.set(setting.getDailyBudget().doubleValue());
        dailyEstimatedCost.set(used == null ? 0D : used.doubleValue());
    }

    private String trimReason(String reason) {
        if (reason == null) return null;
        return reason.length() > 200 ? reason.substring(0, 200) : reason;
    }
}
