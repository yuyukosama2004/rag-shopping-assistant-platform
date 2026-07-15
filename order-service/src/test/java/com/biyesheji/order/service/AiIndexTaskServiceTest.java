package com.biyesheji.order.service;

import com.biyesheji.entity.AiIndexTask;
import com.biyesheji.entity.Product;
import com.biyesheji.order.mapper.AiIndexTaskMapper;
import com.biyesheji.order.mapper.ProductMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AiIndexTaskServiceTest {
    @Test
    void supersedesTaskWhenProductHasNewerVersion() {
        AiIndexTaskMapper tasks = mock(AiIndexTaskMapper.class);
        ProductMapper products = mock(ProductMapper.class);
        AiService aiService = mock(AiService.class);
        LocalDateTime taskVersion = LocalDateTime.of(2026, 7, 15, 10, 0);
        AiIndexTask task = new AiIndexTask();
        task.setId(1L); task.setProductId(2L); task.setOperation("UPSERT");
        task.setProductUpdatedAt(taskVersion); task.setStatus("PENDING"); task.setAttempts(0);
        Product current = new Product(); current.setId(2L); current.setUpdatedAt(taskVersion.plusSeconds(1));
        when(products.selectById(2L)).thenReturn(current);
        when(tasks.claim(1L, 1)).thenReturn(1);

        new AiIndexTaskService(tasks, products, aiService).process(task);

        assertEquals("SUPERSEDED", task.getStatus());
        verifyNoInteractions(aiService);
    }
}
