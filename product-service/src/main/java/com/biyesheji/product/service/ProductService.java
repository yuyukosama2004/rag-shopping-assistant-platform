package com.biyesheji.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biyesheji.entity.Product;
import com.biyesheji.dto.MerchantProductSaveDTO;
import com.biyesheji.dto.MerchantSkuSaveDTO;
import com.biyesheji.dto.MerchantSkuUpdateDTO;
import com.biyesheji.dto.StockAdjustDTO;
import com.biyesheji.entity.ProductSku;
import com.biyesheji.entity.Stock;
import com.biyesheji.entity.StockLedger;

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
    List<ProductSku> listAvailableSkus(Long productId);
    Page<Product> merchantPage(int pageNum, int pageSize, String keyword);
    Product create(MerchantProductSaveDTO dto);
    Product update(Long id, MerchantProductSaveDTO dto);
    Product updateStatus(Long id, Integer status);
    Product copy(Long id);
    void delete(Long id);
    void updateBatchStatus(List<Long> ids, Integer status);
    List<ProductSku> listSkus(Long productId);
    ProductSku createSku(Long productId, Long operatorId, MerchantSkuSaveDTO dto);
    ProductSku updateSku(Long skuId, MerchantSkuUpdateDTO dto);
    Stock getSkuStock(Long skuId);
    Stock adjustSkuStock(Long skuId, Long operatorId, StockAdjustDTO dto);
    List<StockLedger> listSkuStockLedgers(Long skuId);
}
