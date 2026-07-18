package com.biyesheji.order.controller;

import com.biyesheji.constant.UserRole;
import com.biyesheji.dto.R;
import com.biyesheji.exception.BizException;
import com.biyesheji.order.dto.InventoryInsightView;
import com.biyesheji.order.service.InventoryInsightService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/merchant/ai/inventory-insights")
@RequiredArgsConstructor
@Validated
public class MerchantAiInventoryController {

    private final InventoryInsightService service;

    @GetMapping
    public R<InventoryInsightView.Page> list(
            @RequestHeader("X-User-Role") Integer role,
            @RequestParam(defaultValue = "1") @Min(1) int pageNum,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(required = false) InventoryInsightView.Risk risk,
            @RequestParam(defaultValue = "RISK_DESC") InventoryInsightView.Sort sort
    ) {
        requireMerchant(role);
        return R.ok(service.list(pageNum, pageSize, keyword, risk, sort));
    }

    @GetMapping("/summary")
    public R<InventoryInsightView.Summary> summary(
            @RequestHeader("X-User-Role") Integer role
    ) {
        requireMerchant(role);
        return R.ok(service.summary());
    }

    @GetMapping("/{skuId}/evidence")
    public R<InventoryInsightView.Evidence> evidence(
            @RequestHeader("X-User-Role") Integer role,
            @PathVariable Long skuId
    ) {
        requireMerchant(role);
        return R.ok(service.evidence(skuId));
    }

    private void requireMerchant(Integer role) {
        if (!Integer.valueOf(UserRole.OWNER).equals(role)
                && !Integer.valueOf(UserRole.STAFF).equals(role)) {
            throw new BizException(403, "仅店主或店员可查看库存洞察");
        }
    }
}
