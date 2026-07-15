package com.biyesheji.product.controller;

import com.biyesheji.constant.UserRole;
import com.biyesheji.exception.BizException;
import com.biyesheji.product.service.InventoryService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class MerchantInventoryControllerTest {
    private final MerchantInventoryController controller = new MerchantInventoryController(mock(InventoryService.class));

    @Test
    void allowsStaffToAccessInventory() {
        assertDoesNotThrow(() -> controller.page(UserRole.STAFF, 1, 20, null, false, 5));
    }

    @Test
    void rejectsCustomerFromInventory() {
        assertThrows(BizException.class, () -> controller.summary(UserRole.CUSTOMER, 5));
    }
}
