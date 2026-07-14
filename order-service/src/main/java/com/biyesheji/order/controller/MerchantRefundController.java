package com.biyesheji.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biyesheji.constant.UserRole;
import com.biyesheji.dto.R;
import com.biyesheji.dto.RefundProcessDTO;
import com.biyesheji.entity.RefundRecord;
import com.biyesheji.exception.BizException;
import com.biyesheji.order.service.RefundService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/merchant/refunds")
@RequiredArgsConstructor
@Validated
public class MerchantRefundController {
    private final RefundService service;

    @GetMapping
    public R<Page<RefundRecord>> page(@RequestHeader("X-User-Role") Integer role, @RequestParam(defaultValue = "1") @Min(1) int pageNum, @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize, @RequestParam(required = false) String status) {
        requireMerchant(role);
        return R.ok(service.pageForMerchant(pageNum, pageSize, status));
    }

    @PostMapping("/{id}/approve")
    public R<Void> approve(@RequestHeader("X-User-Role") Integer role, @RequestHeader("X-User-Id") Long operatorId, @PathVariable Long id, @Valid @RequestBody RefundProcessDTO dto) {
        requireMerchant(role);
        service.approve(operatorId, id, dto);
        return R.ok();
    }

    @PostMapping("/{id}/reject")
    public R<Void> reject(@RequestHeader("X-User-Role") Integer role, @RequestHeader("X-User-Id") Long operatorId, @PathVariable Long id, @Valid @RequestBody RefundProcessDTO dto) {
        requireMerchant(role);
        service.reject(operatorId, id, dto);
        return R.ok();
    }

    private void requireMerchant(Integer role) {
        if (!Integer.valueOf(UserRole.OWNER).equals(role) && !Integer.valueOf(UserRole.STAFF).equals(role)) throw new BizException(403, "仅店主或店员可处理退款");
    }
}
