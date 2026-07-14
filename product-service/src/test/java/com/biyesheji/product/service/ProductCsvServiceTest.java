package com.biyesheji.product.service;

import com.biyesheji.product.mapper.ProductMapper;
import com.biyesheji.product.mapper.ProductSkuMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class ProductCsvServiceTest {
    @Test
    void returnsEveryInvalidRowBeforeWritingAnyData() {
        ProductMapper products = mock(ProductMapper.class);
        ProductSkuMapper skus = mock(ProductSkuMapper.class);
        ProductService service = mock(ProductService.class);
        ProductCsvService csv = new ProductCsvService(products, skus, service);
        MockMultipartFile file = new MockMultipartFile("file", "products.csv", "text/csv", (ProductCsvService.HEADER + "\nkey,Name,Brand,Phone,0,,Desc,,SKU,{},0,,0\nkey2,Name,Brand,Phone,1,,Desc,,SKU2,{},x,,0").getBytes());

        var result = csv.importCsv(1L, file);

        assertEquals(2, ((java.util.List<?>) result.get("errors")).size());
        verifyNoInteractions(service);
    }
}
