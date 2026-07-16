package com.biyesheji.order.service.impl;

import com.biyesheji.entity.Product;
import com.biyesheji.order.mapper.AiConversationMapper;
import com.biyesheji.order.mapper.ProductMapper;
import com.biyesheji.order.service.AiRetrievalService;
import com.biyesheji.order.service.AiSettingService;
import com.biyesheji.order.service.AiKnowledgeService;
import com.biyesheji.order.service.AiUsageService;
import com.biyesheji.utils.RedisUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class AiServiceImplTest {
    @Test
    void emptyCandidatesForbidInventedRecommendations() {
        AiServiceImpl service = new AiServiceImpl(
                mock(ProductMapper.class),
                mock(AiConversationMapper.class),
                mock(RedisUtil.class),
                Runnable::run,
                mock(AiRetrievalService.class),
                mock(AiSettingService.class),
                mock(AiKnowledgeService.class),
                mock(AiUsageService.class)
        );

        String prompt = service.buildPrompt("推荐手机", List.of());

        assertTrue(prompt.contains("不得自行推荐列表外商品"));
        assertTrue(prompt.contains("联系商家"));
    }
}
