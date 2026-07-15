package com.biyesheji.order.service;

import com.biyesheji.entity.AiSetting;
import com.biyesheji.exception.BizException;
import com.biyesheji.order.mapper.AiRequestLogMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiUsageServiceTest {
    @Test
    void rejectsRequestAfterDailyBudgetIsReached() {
        AiRequestLogMapper mapper = mock(AiRequestLogMapper.class);
        when(mapper.todayCost()).thenReturn(new BigDecimal("5.10"));
        AiSetting setting = new AiSetting();
        setting.setDailyBudget(new BigDecimal("5.00"));

        BizException error = assertThrows(BizException.class,
                () -> new AiUsageService(mapper, new SimpleMeterRegistry()).requireBudget(setting));

        assertEquals(429, error.getCode());
    }

    @Test
    void estimatesTokensWithoutRecordingContent() {
        assertEquals(5, new AiUsageService(mock(AiRequestLogMapper.class), new SimpleMeterRegistry()).estimateTokens(9));
    }

    @Test
    void publishesAiRequestAndDailyCostMetrics() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AiUsageService service = new AiUsageService(mock(AiRequestLogMapper.class), registry);
        service.registerMetrics();
        AiSetting setting = new AiSetting();
        setting.setInputPricePerMillion(new BigDecimal("1.00"));
        setting.setOutputPricePerMillion(new BigDecimal("2.00"));

        service.recordSuccess(setting, 9, 19, 250);

        assertEquals(1D, registry.get("biyesheji.ai.requests").tag("outcome", "success").counter().count());
        assertEquals(0.000025D, registry.get("biyesheji.ai.daily.estimated.cost").gauge().value(), 0.0000001D);
    }
}
