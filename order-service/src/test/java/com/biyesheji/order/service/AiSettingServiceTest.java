package com.biyesheji.order.service;

import com.biyesheji.entity.AiSetting;
import com.biyesheji.exception.BizException;
import com.biyesheji.order.mapper.AiSettingMapper;
import com.biyesheji.utils.RedisUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiSettingServiceTest {
    @Test
    void rejectsChatWhenMerchantDisabledAi() {
        AiSettingMapper mapper = mock(AiSettingMapper.class);
        AiSetting setting = setting(0, 30);
        when(mapper.selectById(1L)).thenReturn(setting);
        AiSettingService service = new AiSettingService(mapper, mock(RedisUtil.class), mock(AiUsageService.class));

        BizException error = assertThrows(BizException.class, () -> service.requireChatAllowed(8L, "推荐手机"));

        assertEquals(503, error.getCode());
    }

    @Test
    void enforcesPerUserDailyLimit() {
        AiSettingMapper mapper = mock(AiSettingMapper.class);
        RedisUtil redis = mock(RedisUtil.class);
        when(mapper.selectById(1L)).thenReturn(setting(1, 2));
        when(redis.increment(anyString())).thenReturn(3L);
        AiSettingService service = new AiSettingService(mapper, redis, mock(AiUsageService.class));

        BizException error = assertThrows(BizException.class, () -> service.requireChatAllowed(8L, "推荐手机"));

        assertEquals(429, error.getCode());
    }

    @Test
    void rejectsMerchantBlockedKeywordBeforeCallingAi() {
        AiSettingMapper mapper = mock(AiSettingMapper.class);
        AiSetting setting = setting(1, 2);
        setting.setBlockedKeywords("赌博, phishing");
        when(mapper.selectById(1L)).thenReturn(setting);
        AiSettingService service = new AiSettingService(mapper, mock(RedisUtil.class), mock(AiUsageService.class));

        BizException error = assertThrows(BizException.class,
                () -> service.requireChatAllowed(8L, "请推荐 PHISHING 工具"));

        assertEquals(400, error.getCode());
    }

    private AiSetting setting(int enabled, int limit) {
        AiSetting setting = new AiSetting();
        setting.setId(1L);
        setting.setEnabled(enabled);
        setting.setPerUserDailyLimit(limit);
        return setting;
    }
}
