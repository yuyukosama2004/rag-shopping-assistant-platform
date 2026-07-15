package com.biyesheji.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.biyesheji.entity.AiIndexTask;
import com.biyesheji.entity.Product;
import com.biyesheji.exception.BizException;
import com.biyesheji.order.mapper.AiIndexTaskMapper;
import com.biyesheji.order.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiIndexTaskService {
    private final AiIndexTaskMapper mapper;
    private final ProductMapper productMapper;
    private final AiService aiService;

    public List<AiIndexTask> listRecent() {
        return mapper.selectList(new LambdaQueryWrapper<AiIndexTask>()
                .orderByDesc(AiIndexTask::getCreatedAt)
                .last("LIMIT 100"));
    }

    public void retry(Long id) {
        AiIndexTask task = mapper.selectById(id);
        if (task == null) throw new BizException(404, "索引任务不存在");
        if (!"FAILED".equals(task.getStatus())) throw new BizException(400, "只有失败任务可以重试");
        task.setStatus("PENDING");
        task.setErrorMessage(null);
        task.setProcessedAt(null);
        mapper.updateById(task);
    }

    @Scheduled(fixedDelayString = "${ai.index.fixed-delay-ms:15000}")
    public void processPending() {
        List<AiIndexTask> pending = mapper.selectList(new LambdaQueryWrapper<AiIndexTask>()
                .eq(AiIndexTask::getStatus, "PENDING")
                .orderByAsc(AiIndexTask::getCreatedAt)
                .last("LIMIT 20"));
        for (AiIndexTask task : pending) process(task);
    }

    void process(AiIndexTask task) {
        int attempts = task.getAttempts() + 1;
        int claimed = mapper.update(null, new LambdaUpdateWrapper<AiIndexTask>()
                .eq(AiIndexTask::getId, task.getId())
                .eq(AiIndexTask::getStatus, "PENDING")
                .set(AiIndexTask::getStatus, "RUNNING")
                .set(AiIndexTask::getAttempts, attempts));
        if (claimed != 1) return;
        task.setStatus("RUNNING");
        task.setAttempts(attempts);
        try {
            Product current = productMapper.selectById(task.getProductId());
            if (isSuperseded(task, current)) {
                finish(task, "SUPERSEDED", null);
                return;
            }
            aiService.refreshProductIndex(task.getProductId(), task.getOperation());
            finish(task, "SUCCESS", null);
        } catch (Exception e) {
            String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            log.warn("AI 索引任务失败: taskId={}, productId={}, message={}", task.getId(), task.getProductId(), message);
            finish(task, "FAILED", message.length() > 500 ? message.substring(0, 500) : message);
        }
    }

    private boolean isSuperseded(AiIndexTask task, Product current) {
        return current != null && current.getUpdatedAt() != null && task.getProductUpdatedAt() != null
                && current.getUpdatedAt().isAfter(task.getProductUpdatedAt());
    }

    private void finish(AiIndexTask task, String status, String error) {
        task.setStatus(status);
        task.setErrorMessage(error);
        task.setProcessedAt(LocalDateTime.now());
        mapper.updateById(task);
    }
}
