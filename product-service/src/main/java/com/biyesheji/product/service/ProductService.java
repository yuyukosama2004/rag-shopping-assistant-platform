package com.biyesheji.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biyesheji.entity.Product;
import com.biyesheji.dto.MerchantProductSaveDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface ProductService {
    Page<Product> page(int pageNum, int pageSize, String brand, String category,
                       BigDecimal minPrice, BigDecimal maxPrice, String keyword, String sort);
    Product getById(Long id);
    List<Product> listByBrand(String brand);
    List<Product> hot(int limit);
    Map<String, List<String>> getFilters();
    Page<Product> merchantPage(int pageNum, int pageSize, String keyword);
    Product create(MerchantProductSaveDTO dto);
    Product update(Long id, MerchantProductSaveDTO dto);
    Product updateStatus(Long id, Integer status);
}
