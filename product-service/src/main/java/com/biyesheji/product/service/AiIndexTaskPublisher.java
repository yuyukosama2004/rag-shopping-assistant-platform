package com.biyesheji.product.service;

import com.biyesheji.entity.AiIndexTask;
import com.biyesheji.entity.Product;
import com.biyesheji.product.mapper.AiIndexTaskMapper;
import com.biyesheji.product.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiIndexTaskPublisher {
    private final AiIndexTaskMapper taskMapper;
    private final ProductMapper productMapper;

    public void publish(Long productId) {
        Product current = productMapper.selectById(productId);
        AiIndexTask task = new AiIndexTask();
        task.setProductId(productId);
        task.setOperation(current != null && Integer.valueOf(1).equals(current.getStatus()) ? "UPSERT" : "DELETE");
        task.setProductUpdatedAt(current == null ? null : current.getUpdatedAt());
        task.setStatus("PENDING");
        task.setAttempts(0);
        taskMapper.insert(task);
    }
}
