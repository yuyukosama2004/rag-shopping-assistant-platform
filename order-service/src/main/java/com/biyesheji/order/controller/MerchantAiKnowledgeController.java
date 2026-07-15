package com.biyesheji.order.controller;

import com.biyesheji.constant.UserRole;
import com.biyesheji.dto.AiKnowledgeSaveDTO;
import com.biyesheji.dto.R;
import com.biyesheji.entity.AiKnowledge;
import com.biyesheji.exception.BizException;
import com.biyesheji.order.service.AiKnowledgeService;
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
@RequestMapping("/api/merchant/ai/knowledge")
@RequiredArgsConstructor
public class MerchantAiKnowledgeController {
    private final AiKnowledgeService service;

    @GetMapping
    public R<List<AiKnowledge>> list(@RequestHeader("X-User-Role") Integer role) {
        requireMerchant(role);
        return R.ok(service.list());
    }

    @PostMapping
    public R<AiKnowledge> create(@RequestHeader("X-User-Role") Integer role,
                                 @Valid @RequestBody AiKnowledgeSaveDTO dto) {
        requireMerchant(role);
        return R.ok(service.create(dto));
    }

    @PutMapping("/{id}")
    public R<AiKnowledge> update(@RequestHeader("X-User-Role") Integer role,
                                 @PathVariable Long id,
                                 @Valid @RequestBody AiKnowledgeSaveDTO dto) {
        requireMerchant(role);
        return R.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@RequestHeader("X-User-Role") Integer role, @PathVariable Long id) {
        requireMerchant(role);
        service.delete(id);
        return R.ok();
    }

    private void requireMerchant(Integer role) {
        if (!Integer.valueOf(UserRole.OWNER).equals(role) && !Integer.valueOf(UserRole.STAFF).equals(role)) {
            throw new BizException(403, "仅商家可管理 AI 知识");
        }
    }
}
