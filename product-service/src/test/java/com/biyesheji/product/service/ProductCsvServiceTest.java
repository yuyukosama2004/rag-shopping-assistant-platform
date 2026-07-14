package com.biyesheji.product.service;

import com.biyesheji.exception.BizException;
import com.biyesheji.product.mapper.ProductMapper;
import com.biyesheji.product.mapper.ProductSkuMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class ProductCsvServiceTest {
    @Test
    void rejectsInvalidCsvBeforeWritingAnyData() {
        ProductMapper products = mock(ProductMapper.class);
        ProductSkuMapper skus = mock(ProductSkuMapper.class);
        ProductService service = mock(ProductService.class);
        ProductCsvService csv = new ProductCsvService(products, skus, service);
        MockMultipartFile file = new MockMultipartFile("file", "products.csv", "text/csv", (ProductCsvService.HEADER + "\nkey,Name,Brand,Phone,0,,Desc,,SKU,{},0,,0").getBytes());

        assertThrows(BizException.class, () -> csv.importCsv(1L, file));
        verifyNoInteractions(service);
    }
}
