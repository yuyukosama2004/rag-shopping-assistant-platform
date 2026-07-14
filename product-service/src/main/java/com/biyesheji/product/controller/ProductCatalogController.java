package com.biyesheji.product.controller;

import com.biyesheji.constant.UserRole;
import com.biyesheji.dto.ProductCatalogSaveDTO;
import com.biyesheji.dto.R;
import com.biyesheji.entity.ProductCatalog;
import com.biyesheji.exception.BizException;
import com.biyesheji.product.service.ProductCatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProductCatalogController {
    private final ProductCatalogService service;
    @GetMapping("/api/product/catalog/{type}") public R<List<ProductCatalog>> publicList(@PathVariable String type) { return R.ok(service.list(type, true)); }
    @GetMapping("/api/merchant/catalog/{type}") public R<List<ProductCatalog>> list(@RequestHeader("X-User-Role") Integer role, @PathVariable String type) { requireMerchant(role); return R.ok(service.list(type, false)); }
    @PostMapping("/api/merchant/catalog/{type}") public R<ProductCatalog> create(@RequestHeader("X-User-Role") Integer role, @PathVariable String type, @Valid @RequestBody ProductCatalogSaveDTO dto) { requireMerchant(role); return R.ok(service.create(type, dto)); }
    @PutMapping("/api/merchant/catalog/{type}/{id}") public R<ProductCatalog> update(@RequestHeader("X-User-Role") Integer role, @PathVariable String type, @PathVariable Long id, @Valid @RequestBody ProductCatalogSaveDTO dto) { requireMerchant(role); return R.ok(service.update(type, id, dto)); }
    @DeleteMapping("/api/merchant/catalog/{type}/{id}") public R<Void> delete(@RequestHeader("X-User-Role") Integer role, @PathVariable String type, @PathVariable Long id) { requireMerchant(role); service.delete(type, id); return R.ok(); }
    private void requireMerchant(Integer role) { if (!Integer.valueOf(UserRole.OWNER).equals(role) && !Integer.valueOf(UserRole.STAFF).equals(role)) throw new BizException(403, "仅店主或店员可管理目录"); }
}
