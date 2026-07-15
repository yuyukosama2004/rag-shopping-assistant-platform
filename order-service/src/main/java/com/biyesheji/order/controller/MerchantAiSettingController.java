package com.biyesheji.order.controller;

import com.biyesheji.constant.UserRole;
import com.biyesheji.dto.AiSettingSaveDTO;
import com.biyesheji.dto.R;
import com.biyesheji.entity.AiSetting;
import com.biyesheji.exception.BizException;
import com.biyesheji.order.service.AiSettingService;
import com.biyesheji.order.service.AiUsageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/merchant/ai/setting")
@RequiredArgsConstructor
public class MerchantAiSettingController {
    private final AiSettingService service;
    private final AiUsageService usageService;

    @GetMapping
    public R<AiSetting> get(@RequestHeader("X-User-Role") Integer role) {
        requireMerchant(role);
        return R.ok(service.get());
    }

    @PutMapping
    public R<AiSetting> update(@RequestHeader("X-User-Role") Integer role,
                               @Valid @RequestBody AiSettingSaveDTO dto) {
        requireOwner(role);
        return R.ok(service.update(dto));
    }

    @GetMapping("/usage")
    public R<java.util.Map<String, Object>> usage(@RequestHeader("X-User-Role") Integer role) {
        requireMerchant(role);
        AiSetting setting = service.get();
        return R.ok(usageService.todaySummary(setting));
    }

    private void requireMerchant(Integer role) {
        if (!Integer.valueOf(UserRole.OWNER).equals(role) && !Integer.valueOf(UserRole.STAFF).equals(role)) {
            throw new BizException(403, "仅商家可查看 AI 设置");
        }
    }

    private void requireOwner(Integer role) {
        if (!Integer.valueOf(UserRole.OWNER).equals(role)) {
            throw new BizException(403, "仅店主可修改 AI 设置");
        }
    }
}
