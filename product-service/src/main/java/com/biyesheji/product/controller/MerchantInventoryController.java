package com.biyesheji.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biyesheji.constant.UserRole;
import com.biyesheji.dto.R;
import com.biyesheji.exception.BizException;
import com.biyesheji.product.service.InventoryService;
import com.biyesheji.vo.MerchantInventorySummaryVO;
import com.biyesheji.vo.MerchantInventoryVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/merchant/inventory")
@RequiredArgsConstructor
@Validated
public class MerchantInventoryController {
    private final InventoryService inventoryService;

    @GetMapping
    public R<Page<MerchantInventoryVO>> page(@RequestHeader("X-User-Role") Integer role,
                                              @RequestParam(defaultValue = "1") @Min(1) int pageNum,
                                              @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(defaultValue = "false") boolean lowStockOnly,
                                              @RequestParam(defaultValue = "${inventory.low-stock-threshold:5}") @Min(0) int threshold) {
        requireMerchant(role);
        return R.ok(inventoryService.page(pageNum, pageSize, keyword, lowStockOnly, threshold));
    }

    @GetMapping("/summary")
    public R<MerchantInventorySummaryVO> summary(@RequestHeader("X-User-Role") Integer role,
                                                  @RequestParam(defaultValue = "${inventory.low-stock-threshold:5}") @Min(0) int threshold) {
        requireMerchant(role);
        return R.ok(inventoryService.summary(threshold));
    }

    private void requireMerchant(Integer role) {
        if (!Integer.valueOf(UserRole.OWNER).equals(role) && !Integer.valueOf(UserRole.STAFF).equals(role)) {
            throw new BizException(403, "仅店主或店员可查看库存");
        }
    }
}
