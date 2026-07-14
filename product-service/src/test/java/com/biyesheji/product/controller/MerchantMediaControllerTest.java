package com.biyesheji.product.controller;

import com.biyesheji.constant.UserRole;
import com.biyesheji.exception.BizException;
import com.biyesheji.product.mapper.ProductMapper;
import com.biyesheji.product.service.MediaStorageService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MerchantMediaControllerTest {
    @Test
    void rejectsDeletingAnImageReferencedByAProduct() {
        MediaStorageService storage = mock(MediaStorageService.class);
        ProductMapper products = mock(ProductMapper.class);
        when(products.selectCount(any())).thenReturn(1L);
        MerchantMediaController controller = new MerchantMediaController(storage, products);

        assertThrows(BizException.class, () -> controller.delete(UserRole.STAFF, "00000000-0000-0000-0000-000000000000.png"));
        verify(storage, never()).delete(any());
    }
}
