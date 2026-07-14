package com.biyesheji.order.controller;

import com.biyesheji.dto.R;
import com.biyesheji.entity.ShippingRule;
import com.biyesheji.order.service.ShippingRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/shipping-rules")
@RequiredArgsConstructor
public class ShippingRuleController {
    private final ShippingRuleService service;

    @GetMapping
    public R<List<ShippingRule>> list() {
        return R.ok(service.list(true));
    }
}
