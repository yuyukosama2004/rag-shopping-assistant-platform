package com.biyesheji.product.controller;

import com.biyesheji.constant.UserRole;
import com.biyesheji.exception.BizException;
import com.biyesheji.product.service.ProductService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class MerchantProductControllerTest {

    private final MerchantProductController controller = new MerchantProductController(mock(ProductService.class));

    @Test
    void allowsStaffToAccessProductWorkspace() {
        assertDoesNotThrow(() -> controller.page(UserRole.STAFF, 1, 20, null));
    }

    @Test
    void rejectsCustomerFromProductWorkspace() {
        assertThrows(BizException.class, () -> controller.page(UserRole.CUSTOMER, 1, 20, null));
    }
}
