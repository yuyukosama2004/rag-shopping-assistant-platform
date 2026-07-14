package com.biyesheji.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biyesheji.constant.UserRole;
import com.biyesheji.dto.MerchantProductSaveDTO;
import com.biyesheji.dto.MerchantProductStatusDTO;
import com.biyesheji.dto.MerchantSkuSaveDTO;
import com.biyesheji.dto.StockAdjustDTO;
import com.biyesheji.dto.R;
import com.biyesheji.entity.Product;
import com.biyesheji.entity.ProductSku;
import com.biyesheji.entity.Stock;
import com.biyesheji.entity.StockLedger;
import com.biyesheji.exception.BizException;
import com.biyesheji.product.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/merchant/products")
@RequiredArgsConstructor
@Validated
public class MerchantProductController {
    private final ProductService productService;

    @GetMapping
    public R<Page<Product>> page(@RequestHeader("X-User-Role") Integer role, @RequestParam(defaultValue = "1") @Min(1) int pageNum, @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize, @RequestParam(required = false) String keyword) {
        requireMerchant(role); return R.ok(productService.merchantPage(pageNum, pageSize, keyword));
    }
    @PostMapping
    public R<Product> create(@RequestHeader("X-User-Role") Integer role, @Valid @RequestBody MerchantProductSaveDTO dto) { requireMerchant(role); return R.ok(productService.create(dto)); }
    @PutMapping("/{id}")
    public R<Product> update(@RequestHeader("X-User-Role") Integer role, @PathVariable Long id, @Valid @RequestBody MerchantProductSaveDTO dto) { requireMerchant(role); return R.ok(productService.update(id, dto)); }
    @PutMapping("/{id}/status")
    public R<Product> status(@RequestHeader("X-User-Role") Integer role, @PathVariable Long id, @Valid @RequestBody MerchantProductStatusDTO dto) { requireMerchant(role); return R.ok(productService.updateStatus(id, dto.getStatus())); }
    @GetMapping("/{id}/skus")
    public R<List<ProductSku>> skus(@RequestHeader("X-User-Role") Integer role, @PathVariable Long id) { requireMerchant(role); return R.ok(productService.listSkus(id)); }
    @PostMapping("/{id}/skus")
    public R<ProductSku> createSku(@RequestHeader("X-User-Role") Integer role, @RequestHeader("X-User-Id") Long userId, @PathVariable Long id, @Valid @RequestBody MerchantSkuSaveDTO dto) { requireMerchant(role); return R.ok(productService.createSku(id, userId, dto)); }
    @GetMapping("/skus/{skuId}/stock")
    public R<Stock> stock(@RequestHeader("X-User-Role") Integer role, @PathVariable Long skuId) { requireMerchant(role); return R.ok(productService.getSkuStock(skuId)); }
    @PutMapping("/skus/{skuId}/stock")
    public R<Stock> adjustStock(@RequestHeader("X-User-Role") Integer role, @RequestHeader("X-User-Id") Long userId, @PathVariable Long skuId, @Valid @RequestBody StockAdjustDTO dto) { requireMerchant(role); return R.ok(productService.adjustSkuStock(skuId, userId, dto)); }
    @GetMapping("/skus/{skuId}/stock/ledger")
    public R<List<StockLedger>> stockLedgers(@RequestHeader("X-User-Role") Integer role, @PathVariable Long skuId) { requireMerchant(role); return R.ok(productService.listSkuStockLedgers(skuId)); }
    private void requireMerchant(Integer role) {
        if (!Integer.valueOf(UserRole.OWNER).equals(role) && !Integer.valueOf(UserRole.STAFF).equals(role)) {
            throw new BizException(403, "仅店主或店员可管理商品");
        }
    }
}
