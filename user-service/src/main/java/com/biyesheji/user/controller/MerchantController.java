package com.biyesheji.user.controller;

import com.biyesheji.dto.OwnerInitializeDTO;
import com.biyesheji.dto.R;
import com.biyesheji.dto.StoreSettingUpdateDTO;
import com.biyesheji.dto.StaffCreateDTO;
import com.biyesheji.dto.StaffStatusUpdateDTO;
import com.biyesheji.entity.StoreSetting;
import com.biyesheji.entity.User;
import com.biyesheji.user.service.MerchantService;
import com.biyesheji.utils.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @Operation(summary = "查询店员")
    @GetMapping("/staff")
    public R<List<User>> listStaff(@RequestHeader("Authorization") String authHeader) {
        return R.ok(merchantService.listStaff(accessUserId(authHeader)));
    }

    @Operation(summary = "创建店员")
    @PostMapping("/staff")
    public R<User> createStaff(@RequestHeader("Authorization") String authHeader,
                               @Valid @RequestBody StaffCreateDTO dto) {
        return R.ok(merchantService.createStaff(accessUserId(authHeader), dto));
    }

    @Operation(summary = "启用或禁用店员")
    @PutMapping("/staff/{staffId}/status")
    public R<User> updateStaffStatus(@RequestHeader("Authorization") String authHeader,
                                     @PathVariable Long staffId,
                                     @Valid @RequestBody StaffStatusUpdateDTO dto) {
        return R.ok(merchantService.updateStaffStatus(accessUserId(authHeader), staffId, dto));
    }

    private Long accessUserId(String authHeader) {
        return jwtUtil.getAccessUserId(authHeader.replace("Bearer ", ""));
    }
}
