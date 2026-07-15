package com.biyesheji.order.service;

import com.biyesheji.entity.AiRequestLog;
import com.biyesheji.entity.AiSetting;
import com.biyesheji.exception.BizException;
import com.biyesheji.order.mapper.AiRequestLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiUsageService {
    private static final BigDecimal ONE_MILLION = new BigDecimal("1000000");
    private final AiRequestLogMapper mapper;

    public void requireBudget(AiSetting setting) {
        if (setting.getDailyBudget().signum() == 0) return;
        BigDecimal used = mapper.todayCost();
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
        } catch (Exception e) {
            log.warn("记录 AI 请求指标失败: {}", e.getMessage());
        }
    }

    int estimateTokens(int chars) {
        return chars <= 0 ? 0 : (chars + 1) / 2;
    }

    private String trimReason(String reason) {
        if (reason == null) return null;
        return reason.length() > 200 ? reason.substring(0, 200) : reason;
    }
}
