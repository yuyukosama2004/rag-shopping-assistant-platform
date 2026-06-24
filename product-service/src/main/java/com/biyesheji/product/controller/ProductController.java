package com.biyesheji.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biyesheji.dto.R;
import com.biyesheji.entity.Product;
import com.biyesheji.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Tag(name = "商品接口", description = "商品查询、搜索、筛选")
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "商品分页列表")
    @GetMapping("/page")
    public R<Page<Product>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "12") int pageSize,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "sales") String sort) {
        return R.ok(productService.page(pageNum, pageSize, brand, category,
                minPrice, maxPrice, keyword, sort));
    }

    @Operation(summary = "商品详情")
    @GetMapping("/{id}")
    public R<Product> detail(@PathVariable Long id) {
        return R.ok(productService.getById(id));
    }

    @Operation(summary = "热门推荐")
    @GetMapping("/hot")
    public R<List<Product>> hot(@RequestParam(defaultValue = "8") int limit) {
        return R.ok(productService.hot(limit));
    }

    @Operation(summary = "筛选选项（品牌、分类）")
    @GetMapping("/filters")
    public R<Map<String, List<String>>> filters() {
        return R.ok(productService.getFilters());
    }
}
