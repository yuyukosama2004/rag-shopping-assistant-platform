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
    private final AiUsageService aiUsageService;

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
        setting.setDailyBudget(dto.getDailyBudget());
        setting.setInputPricePerMillion(dto.getInputPricePerMillion());
        setting.setOutputPricePerMillion(dto.getOutputPricePerMillion());
        setting.setBlockedKeywords(trimToNull(dto.getBlockedKeywords()));
        setting.setDisclaimer(trimToNull(dto.getDisclaimer()));
        setting.setSystemPrompt(trimToNull(dto.getSystemPrompt()));
        mapper.updateById(setting);
        return setting;
    }

    public AiSetting requireChatAllowed(Long userId, String query) {
        AiSetting setting = get();
        if (!Integer.valueOf(1).equals(setting.getEnabled())) {
            throw new BizException(503, "AI 导购已由商家暂停服务");
        }
        if (userId == null) throw new BizException(401, "请登录后使用 AI 导购");
        requireSafeInput(setting, query);
        aiUsageService.requireBudget(setting);

        String key = "ai:quota:" + LocalDate.now() + ":" + userId;
        long used = redisUtil.increment(key);
        if (used == 1) redisUtil.expire(key, 2, TimeUnit.DAYS);
        if (used > setting.getPerUserDailyLimit()) {
            throw new BizException(429, "今日 AI 咨询次数已用完，请明天再试或联系商家");
        }
        return setting;
    }

    private void requireSafeInput(AiSetting setting, String query) {
        if (setting.getBlockedKeywords() == null) return;
        String normalizedQuery = query.toLowerCase(java.util.Locale.ROOT);
        for (String keyword : setting.getBlockedKeywords().split("[,，\\n]")) {
            String normalized = keyword.trim().toLowerCase(java.util.Locale.ROOT);
            if (!normalized.isEmpty() && normalizedQuery.contains(normalized)) {
                throw new BizException(400, "咨询内容包含商家设置的敏感词，请修改后重试");
            }
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
