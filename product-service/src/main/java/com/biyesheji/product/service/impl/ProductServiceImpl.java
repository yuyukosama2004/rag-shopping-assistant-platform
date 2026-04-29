package com.biyesheji.product.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biyesheji.entity.Product;
import com.biyesheji.exception.BizException;
import com.biyesheji.product.mapper.ProductMapper;
import com.biyesheji.product.service.ProductService;
import com.biyesheji.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;
    private final RedisUtil redisUtil;

    private static final String CACHE_PRODUCT = "product:detail:";
    private static final String CACHE_HOT = "product:hot";
    private static final String CACHE_FILTERS = "product:filters";
    private static final String NULL_PLACEHOLDER = "__NULL__";

    @Override
    public Page<Product> page(int pageNum, int pageSize, String brand, String category,
                              BigDecimal minPrice, BigDecimal maxPrice, String keyword, String sort) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, 1);

        if (brand != null && !brand.isEmpty()) {
            wrapper.eq(Product::getBrand, brand);
        }
        if (category != null && !category.isEmpty()) {
            wrapper.eq(Product::getCategory, category);
        }
        if (minPrice != null) {
            wrapper.ge(Product::getPrice, minPrice);
        }
        if (maxPrice != null) {
            wrapper.le(Product::getPrice, maxPrice);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Product::getName, keyword)
                    .or().like(Product::getBrand, keyword)
                    .or().like(Product::getDescription, keyword));
        }

        // 排序
        if ("price_asc".equals(sort)) {
            wrapper.orderByAsc(Product::getPrice);
        } else if ("price_desc".equals(sort)) {
            wrapper.orderByDesc(Product::getPrice);
        } else if ("sales".equals(sort)) {
            wrapper.orderByDesc(Product::getSales);
        } else {
            wrapper.orderByDesc(Product::getSales);
        }

        return productMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public Product getById(Long id) {
        String cacheKey = CACHE_PRODUCT + id;
        Product cached = redisUtil.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        // 空值防穿透：检查是否缓存了空值标记
        if (NULL_PLACEHOLDER.equals(redisUtil.get(cacheKey + ":null"))) {
            throw new BizException(404, "商品不存在或已下架");
        }

        Product product = productMapper.selectById(id);
        if (product == null || product.getStatus() == 0) {
            // 缓存空值标记 5 分钟，防止缓存穿透
            redisUtil.set(cacheKey + ":null", NULL_PLACEHOLDER, 5, TimeUnit.MINUTES);
            throw new BizException(404, "商品不存在或已下架");
        }

        redisUtil.set(cacheKey, product, 30, TimeUnit.MINUTES);
        return product;
    }

    @Override
    public List<Product> listByBrand(String brand) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getBrand, brand)
                .eq(Product::getStatus, 1)
                .orderByDesc(Product::getSales);
        return productMapper.selectList(wrapper);
    }

    @Override
    public List<Product> hot(int limit) {
        // 先查缓存
        String cacheKey = CACHE_HOT + ":" + limit;
        String cached = redisUtil.get(cacheKey);
        if (cached != null) {
            return JSONUtil.toList(cached, Product.class);
        }

        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, 1)
                .orderByDesc(Product::getSales)
                .last("LIMIT " + limit);

        List<Product> products = productMapper.selectList(wrapper);

        // 缓存 10 分钟
        redisUtil.set(cacheKey, JSONUtil.toJsonStr(products), 10, TimeUnit.MINUTES);
        return products;
    }

    @Override
    public Map<String, List<String>> getFilters() {
        Map<String, List<String>> cached = redisUtil.get(CACHE_FILTERS);
        if (cached != null) {
            return cached;
        }

        Map<String, List<String>> filters = new HashMap<>();

        // 使用 SQL DISTINCT 避免全量加载数据到内存
        LambdaQueryWrapper<Product> brandWrapper = new LambdaQueryWrapper<>();
        brandWrapper.select(Product::getBrand).eq(Product::getStatus, 1)
                .groupBy(Product::getBrand);
        List<String> brands = productMapper.selectList(brandWrapper).stream()
                .map(Product::getBrand).distinct().collect(Collectors.toList());

        LambdaQueryWrapper<Product> catWrapper = new LambdaQueryWrapper<>();
        catWrapper.select(Product::getCategory).eq(Product::getStatus, 1)
                .groupBy(Product::getCategory);
        List<String> categories = productMapper.selectList(catWrapper).stream()
                .map(Product::getCategory).distinct().collect(Collectors.toList());

        filters.put("brands", brands);
        filters.put("categories", categories);

        // 缓存 30 分钟
        redisUtil.set(CACHE_FILTERS, filters, 30, TimeUnit.MINUTES);
        return filters;
    }
}
