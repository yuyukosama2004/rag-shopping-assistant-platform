package com.biyesheji.user.controller;

import com.biyesheji.dto.OwnerInitializeDTO;
import com.biyesheji.dto.R;
import com.biyesheji.dto.StoreSettingUpdateDTO;
import com.biyesheji.entity.StoreSetting;
import com.biyesheji.user.service.MerchantService;
import com.biyesheji.utils.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "商家店铺接口", description = "店主初始化和店铺配置")
@RestController
@RequestMapping("/api/merchant")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;
    private final JwtUtil jwtUtil;

    @Operation(summary = "初始化唯一店主和店铺")
    @PostMapping("/initialize")
    public R<Void> initialize(@RequestHeader("X-Owner-Init-Token") String initToken,
                              @Valid @RequestBody OwnerInitializeDTO dto) {
        merchantService.initializeOwner(initToken, dto);
        return R.ok();
    }

    @Operation(summary = "查询店铺配置")
    @GetMapping("/store/setting")
    public R<StoreSetting> getStoreSetting(@RequestHeader("Authorization") String authHeader) {
        Long userId = jwtUtil.getAccessUserId(authHeader.replace("Bearer ", ""));
        return R.ok(merchantService.getMerchantStoreSetting(userId));
    }

    @Operation(summary = "更新店铺配置")
    @PutMapping("/store/setting")
    public R<StoreSetting> updateStoreSetting(@RequestHeader("Authorization") String authHeader,
                                              @Valid @RequestBody StoreSettingUpdateDTO dto) {
        Long userId = jwtUtil.getAccessUserId(authHeader.replace("Bearer ", ""));
        return R.ok(merchantService.updateStoreSetting(userId, dto));
    }
}
