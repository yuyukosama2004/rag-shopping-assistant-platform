package com.biyesheji.order.controller;

import com.biyesheji.constant.UserRole;
import com.biyesheji.dto.R;
import com.biyesheji.entity.AiIndexTask;
import com.biyesheji.exception.BizException;
import com.biyesheji.order.service.AiIndexTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/merchant/ai/index-tasks")
@RequiredArgsConstructor
public class MerchantAiIndexTaskController {
    private final AiIndexTaskService service;

    @GetMapping
    public R<List<AiIndexTask>> list(@RequestHeader("X-User-Role") Integer role) {
        requireMerchant(role);
        return R.ok(service.listRecent());
    }

    @PostMapping("/{id}/retry")
    public R<Void> retry(@RequestHeader("X-User-Role") Integer role, @PathVariable Long id) {
        requireMerchant(role);
        service.retry(id);
        return R.ok();
    }

    private void requireMerchant(Integer role) {
        if (!Integer.valueOf(UserRole.OWNER).equals(role) && !Integer.valueOf(UserRole.STAFF).equals(role)) {
            throw new BizException(403, "仅商家可查看 AI 索引任务");
        }
    }
}
