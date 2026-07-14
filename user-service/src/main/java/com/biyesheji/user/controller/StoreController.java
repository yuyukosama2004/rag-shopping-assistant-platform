package com.biyesheji.user.controller;

import com.biyesheji.dto.R;
import com.biyesheji.entity.StoreSetting;
import com.biyesheji.user.service.MerchantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "店铺公开接口", description = "消费者读取店铺信息")
@RestController
@RequestMapping("/api/store")
@RequiredArgsConstructor
public class StoreController {

    private final MerchantService merchantService;

    @Operation(summary = "读取店铺公开配置")
    @GetMapping("/setting")
    public R<StoreSetting> getStoreSetting() {
        return R.ok(merchantService.getStoreSetting());
    }
}
