package com.biyesheji.product.service;

import com.biyesheji.dto.ProductCatalogSaveDTO;
import com.biyesheji.entity.ProductCatalog;
import com.biyesheji.exception.BizException;
import com.biyesheji.product.mapper.ProductCatalogMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProductCatalogServiceTest {
    @Test
    void createsTrimmedCatalogItem() {
        ProductCatalogMapper mapper = mock(ProductCatalogMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of());
        doAnswer(invocation -> { invocation.<ProductCatalog>getArgument(0).setId(1L); return 1; }).when(mapper).insert(any(ProductCatalog.class));
        ProductCatalogService service = new ProductCatalogService(mapper);

        ProductCatalog item = service.create("BRAND", dto("  Acme  ", 3, 1));

        assertEquals("Acme", item.getName());
        assertEquals("BRAND", item.getCatalogType());
    }

    @Test
    void rejectsUnknownCatalogType() {
        ProductCatalogService service = new ProductCatalogService(mock(ProductCatalogMapper.class));

        assertThrows(BizException.class, () -> service.list("TAG", true));
    }

    @Test
    void disablingPreservesTheCatalogRecord() {
        ProductCatalogMapper mapper = mock(ProductCatalogMapper.class);
        ProductCatalog item = new ProductCatalog();
        item.setId(1L); item.setCatalogType("CATEGORY"); item.setStatus(1);
        when(mapper.selectById(1L)).thenReturn(item);
        ProductCatalogService service = new ProductCatalogService(mapper);

        service.delete("CATEGORY", 1L);

        assertEquals(0, item.getStatus());
        verify(mapper).updateById(item);
        verify(mapper, never()).deleteById(any(java.io.Serializable.class));
    }

    private ProductCatalogSaveDTO dto(String name, int sortOrder, int status) {
        ProductCatalogSaveDTO dto = new ProductCatalogSaveDTO();
        dto.setName(name); dto.setSortOrder(sortOrder); dto.setStatus(status);
        return dto;
    }
}
