package com.biyesheji.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.biyesheji.dto.ShippingRuleSaveDTO;
import com.biyesheji.entity.ShippingRule;
import com.biyesheji.exception.BizException;
import com.biyesheji.order.mapper.ShippingRuleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShippingRuleService {
    private final ShippingRuleMapper mapper;

    public List<ShippingRule> list(boolean activeOnly) {
        LambdaQueryWrapper<ShippingRule> query = new LambdaQueryWrapper<ShippingRule>().orderByAsc(ShippingRule::getSortOrder).orderByAsc(ShippingRule::getId);
        if (activeOnly) query.eq(ShippingRule::getStatus, 1);
        return mapper.selectList(query);
    }

    public ShippingRule create(ShippingRuleSaveDTO dto) {
        ShippingRule rule = new ShippingRule();
        copy(dto, rule);
        mapper.insert(rule);
        return rule;
    }

    public ShippingRule update(Long id, ShippingRuleSaveDTO dto) {
        ShippingRule rule = require(id);
        copy(dto, rule);
        mapper.updateById(rule);
        return rule;
    }

    public void disable(Long id) {
        ShippingRule rule = require(id);
        rule.setStatus(0);
        mapper.updateById(rule);
    }

    public ShippingRule requireActive(Long id) {
        ShippingRule rule = require(id);
        if (rule.getStatus() != 1) throw new BizException(400, "配送方式不可用");
        return rule;
    }

    public BigDecimal calculateFee(ShippingRule rule, BigDecimal productAmount) {
        if ("PICKUP".equals(rule.getRuleType())) return BigDecimal.ZERO;
        if (rule.getFreeShippingThreshold() != null && productAmount.compareTo(rule.getFreeShippingThreshold()) >= 0) return BigDecimal.ZERO;
        return rule.getBaseFee();
    }

    private ShippingRule require(Long id) {
        ShippingRule rule = mapper.selectById(id);
        if (rule == null) throw new BizException(404, "配送规则不存在");
        return rule;
    }

    private void copy(ShippingRuleSaveDTO dto, ShippingRule rule) {
        rule.setRuleType(dto.getRuleType());
        rule.setName(dto.getName().trim());
        rule.setBaseFee(dto.getBaseFee());
        rule.setFreeShippingThreshold(dto.getFreeShippingThreshold());
        rule.setStatus(dto.getStatus());
        rule.setSortOrder(dto.getSortOrder());
    }
}
