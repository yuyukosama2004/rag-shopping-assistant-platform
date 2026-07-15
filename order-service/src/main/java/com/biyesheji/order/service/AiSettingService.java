package com.biyesheji.order.service;

import com.biyesheji.dto.AiSettingSaveDTO;
import com.biyesheji.entity.AiSetting;
import com.biyesheji.exception.BizException;
import com.biyesheji.order.mapper.AiSettingMapper;
import com.biyesheji.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AiSettingService {
    private static final long SETTING_ID = 1L;

    private final AiSettingMapper mapper;
    private final RedisUtil redisUtil;

    public AiSetting get() {
        AiSetting setting = mapper.selectById(SETTING_ID);
        if (setting == null) throw new BizException(503, "AI 设置尚未初始化");
        return setting;
    }

    public AiSetting update(AiSettingSaveDTO dto) {
        AiSetting setting = get();
        setting.setEnabled(dto.getEnabled());
        setting.setModel(dto.getModel().trim());
        setting.setTemperature(dto.getTemperature());
        setting.setMaxOutputTokens(dto.getMaxOutputTokens());
        setting.setPerUserDailyLimit(dto.getPerUserDailyLimit());
        setting.setDisclaimer(trimToNull(dto.getDisclaimer()));
        setting.setSystemPrompt(trimToNull(dto.getSystemPrompt()));
        mapper.updateById(setting);
        return setting;
    }

    public AiSetting requireChatAllowed(Long userId) {
        AiSetting setting = get();
        if (!Integer.valueOf(1).equals(setting.getEnabled())) {
            throw new BizException(503, "AI 导购已由商家暂停服务");
        }
        if (userId == null) throw new BizException(401, "请登录后使用 AI 导购");

        String key = "ai:quota:" + LocalDate.now() + ":" + userId;
        long used = redisUtil.increment(key);
        if (used == 1) redisUtil.expire(key, 2, TimeUnit.DAYS);
        if (used > setting.getPerUserDailyLimit()) {
            throw new BizException(429, "今日 AI 咨询次数已用完，请明天再试或联系商家");
        }
        return setting;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
