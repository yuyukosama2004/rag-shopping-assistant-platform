package com.biyesheji.order.service;

import com.biyesheji.entity.AiSetting;
import com.biyesheji.exception.BizException;
import com.biyesheji.order.mapper.AiRequestLogMapper;
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
                () -> new AiUsageService(mapper).requireBudget(setting));

        assertEquals(429, error.getCode());
    }

    @Test
    void estimatesTokensWithoutRecordingContent() {
        assertEquals(5, new AiUsageService(mock(AiRequestLogMapper.class)).estimateTokens(9));
    }
}
