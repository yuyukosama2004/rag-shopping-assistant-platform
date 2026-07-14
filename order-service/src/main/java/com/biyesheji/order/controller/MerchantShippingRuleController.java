package com.biyesheji.order.controller;

import com.biyesheji.constant.UserRole;
import com.biyesheji.dto.R;
import com.biyesheji.dto.ShippingRuleSaveDTO;
import com.biyesheji.entity.ShippingRule;
import com.biyesheji.exception.BizException;
import com.biyesheji.order.service.ShippingRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/merchant/shipping-rules")
@RequiredArgsConstructor
public class MerchantShippingRuleController {
    private final ShippingRuleService service;

    @GetMapping
    public R<List<ShippingRule>> list(@RequestHeader("X-User-Role") Integer role) {
        requireMerchant(role);
        return R.ok(service.list(false));
    }

    @PostMapping
    public R<ShippingRule> create(@RequestHeader("X-User-Role") Integer role, @Valid @RequestBody ShippingRuleSaveDTO dto) {
        requireMerchant(role);
        return R.ok(service.create(dto));
    }

    @PutMapping("/{id}")
    public R<ShippingRule> update(@RequestHeader("X-User-Role") Integer role, @PathVariable Long id, @Valid @RequestBody ShippingRuleSaveDTO dto) {
        requireMerchant(role);
        return R.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public R<Void> disable(@RequestHeader("X-User-Role") Integer role, @PathVariable Long id) {
        requireMerchant(role);
        service.disable(id);
        return R.ok();
    }

    private void requireMerchant(Integer role) {
        if (!Integer.valueOf(UserRole.OWNER).equals(role) && !Integer.valueOf(UserRole.STAFF).equals(role)) {
            throw new BizException(403, "仅店主或店员可管理配送规则");
        }
    }
}
