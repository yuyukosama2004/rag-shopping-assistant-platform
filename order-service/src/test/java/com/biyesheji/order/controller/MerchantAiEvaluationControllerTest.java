package com.biyesheji.order.controller;

import com.biyesheji.constant.UserRole;
import com.biyesheji.entity.Product;
import com.biyesheji.exception.BizException;
import com.biyesheji.order.dto.AiEvaluationRequest;
import com.biyesheji.order.dto.AiEvaluationResponse;
import com.biyesheji.order.service.AiRetrievalItem;
import com.biyesheji.order.service.AiRetrievalResult;
import com.biyesheji.order.service.AiRetrievalService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MerchantAiEvaluationControllerTest {

    @Test
    void disabledEndpointDoesNotRunRetrieval() {
        AiRetrievalService retrieval = mock(AiRetrievalService.class);
        MerchantAiEvaluationController controller = new MerchantAiEvaluationController(
                retrieval,
                false,
                "chat-model",
                "embedding-model"
        );

        BizException error = assertThrows(
                BizException.class,
                () -> controller.evaluate(UserRole.OWNER, request("camera phone"))
        );

        assertEquals(404, error.getCode());
        verifyNoInteractions(retrieval);
    }

    @Test
    void merchantReceivesReadOnlyRetrievalEnvelope() {
        AiRetrievalService retrieval = mock(AiRetrievalService.class);
        Product product = new Product();
        product.setId(18L);
        product.setBrand("Xiaomi");
        product.setPrice(BigDecimal.valueOf(3899));
        AiRetrievalItem item = new AiRetrievalItem(product, 0.91, 1);
        when(retrieval.retrieve("camera phone", 10)).thenReturn(
                new AiRetrievalResult(
                        List.of(item),
                        List.of(item),
                        "vector",
                        true,
                        "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                )
        );
        MerchantAiEvaluationController controller = new MerchantAiEvaluationController(
                retrieval,
                true,
                "chat-model",
                "embedding-model"
        );

        AiEvaluationResponse response = controller.evaluate(
                UserRole.STAFF,
                request(" camera phone ")
        );

        assertEquals(List.of(18L), response.eligibleProducts().stream()
                .map(AiEvaluationResponse.ProductResult::id)
                .toList());
        assertEquals(List.of(), response.recommendedIds());
        assertNull(response.answer());
        assertEquals("unavailable", response.usage().tokenCountKind());
        assertEquals("vector", response.versions().retrievalMode());
        assertFalse(response.retrievedProducts().isEmpty());
        verify(retrieval).retrieve("camera phone", 10);
    }

    @Test
    void customersCannotRunEvaluation() {
        AiRetrievalService retrieval = mock(AiRetrievalService.class);
        MerchantAiEvaluationController controller = new MerchantAiEvaluationController(
                retrieval,
                true,
                "chat-model",
                "embedding-model"
        );

        BizException error = assertThrows(
                BizException.class,
                () -> controller.evaluate(UserRole.CUSTOMER, request("camera phone"))
        );

        assertEquals(403, error.getCode());
        verifyNoInteractions(retrieval);
    }

    private AiEvaluationRequest request(String query) {
        AiEvaluationRequest request = new AiEvaluationRequest();
        request.setQuery(query);
        return request;
    }
}
