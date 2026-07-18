package com.biyesheji.order.controller;

import com.biyesheji.constant.UserRole;
import com.biyesheji.exception.BizException;
import com.biyesheji.order.dto.InventoryInsightView;
import com.biyesheji.order.service.InventoryInsightService;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MerchantAiInventoryControllerTest {

    @Test
    void ownerAndStaffCanReadDeterministicInsights() {
        InventoryInsightService service = mock(InventoryInsightService.class);
        InventoryInsightView.Page page = new InventoryInsightView.Page(List.of(), 0, 1, 20, 0);
        InventoryInsightView.Summary summary = new InventoryInsightView.Summary(
                Map.of(),
                0,
                0,
                0,
                OffsetDateTime.parse("2026-07-18T12:00:00+08:00")
        );
        when(service.list(1, 20, null, null, InventoryInsightView.Sort.RISK_DESC))
                .thenReturn(page);
        when(service.summary()).thenReturn(summary);
        MerchantAiInventoryController controller = new MerchantAiInventoryController(service);

        assertEquals(page, controller.list(
                UserRole.OWNER,
                1,
                20,
                null,
                null,
                InventoryInsightView.Sort.RISK_DESC
        ).getData());
        assertEquals(summary, controller.summary(UserRole.STAFF).getData());

        verify(service).list(1, 20, null, null, InventoryInsightView.Sort.RISK_DESC);
        verify(service).summary();
    }

    @Test
    void customerAndAnonymousRequestsAreRejectedBeforeServiceAccess() {
        InventoryInsightService service = mock(InventoryInsightService.class);
        MerchantAiInventoryController controller = new MerchantAiInventoryController(service);

        BizException customerError = assertThrows(
                BizException.class,
                () -> controller.summary(UserRole.CUSTOMER)
        );
        BizException anonymousError = assertThrows(
                BizException.class,
                () -> controller.summary(null)
        );

        assertEquals(403, customerError.getCode());
        assertEquals(403, anonymousError.getCode());
        verifyNoInteractions(service);
    }

    @Test
    void legacyNegativeSkuIdsRemainQueryable() {
        InventoryInsightService service = mock(InventoryInsightService.class);
        MerchantAiInventoryController controller = new MerchantAiInventoryController(service);

        assertNull(controller.evidence(UserRole.OWNER, -9L).getData());

        verify(service).evidence(-9L);
    }
}
