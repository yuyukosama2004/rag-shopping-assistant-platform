package com.biyesheji.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.biyesheji.dto.ProductCatalogSaveDTO;
import com.biyesheji.entity.ProductCatalog;
import com.biyesheji.exception.BizException;
import com.biyesheji.product.mapper.ProductCatalogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductCatalogService {
    private final ProductCatalogMapper mapper;

    public List<ProductCatalog> list(String type, boolean activeOnly) {
        LambdaQueryWrapper<ProductCatalog> query = new LambdaQueryWrapper<ProductCatalog>().eq(ProductCatalog::getCatalogType, type(type));
        if (activeOnly) query.eq(ProductCatalog::getStatus, 1);
        return mapper.selectList(query.orderByAsc(ProductCatalog::getSortOrder).orderByAsc(ProductCatalog::getName));
    }
    public ProductCatalog create(String type, ProductCatalogSaveDTO dto) {
        ensureUnique(type, dto.getName(), null);
        ProductCatalog item = new ProductCatalog(); item.setCatalogType(type(type)); item.setName(dto.getName().trim()); item.setSortOrder(dto.getSortOrder()); item.setStatus(dto.getStatus()); mapper.insert(item); return item;
    }
    public ProductCatalog update(String type, Long id, ProductCatalogSaveDTO dto) {
        ProductCatalog item = require(type, id); ensureUnique(type, dto.getName(), id);
        item.setName(dto.getName().trim()); item.setSortOrder(dto.getSortOrder()); item.setStatus(dto.getStatus()); mapper.updateById(item); return item;
    }
    public void delete(String type, Long id) {
        ProductCatalog item = require(type, id);
        item.setStatus(0);
        mapper.updateById(item);
    }
    private ProductCatalog require(String type, Long id) { ProductCatalog item = mapper.selectById(id); if (item == null || !type(type).equals(item.getCatalogType())) throw new BizException(404, "目录项不存在"); return item; }
    private void ensureUnique(String type, String name, Long currentId) { for (ProductCatalog item : mapper.selectList(new LambdaQueryWrapper<ProductCatalog>().eq(ProductCatalog::getCatalogType, type(type)).eq(ProductCatalog::getName, name.trim()))) if (!item.getId().equals(currentId)) throw new BizException(400, "目录项已存在"); }
    private String type(String value) { if (!"BRAND".equals(value) && !"CATEGORY".equals(value)) throw new BizException(400, "目录类型无效"); return value; }
}
