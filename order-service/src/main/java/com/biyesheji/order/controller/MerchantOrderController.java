package com.biyesheji.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biyesheji.constant.UserRole;
import com.biyesheji.dto.MerchantCloseOrderDTO;
import com.biyesheji.dto.MerchantOrderNoteDTO;
import com.biyesheji.dto.MerchantShipmentDTO;
import com.biyesheji.dto.R;
import com.biyesheji.exception.BizException;
import com.biyesheji.order.service.OrderService;
import com.biyesheji.vo.OrderVO;
import com.biyesheji.vo.MerchantDashboardVO;
import com.biyesheji.vo.MerchantOrderDetailVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/merchant/orders")
@RequiredArgsConstructor
@Validated
public class MerchantOrderController {
    private final OrderService orderService;

    @GetMapping
    public R<Page<OrderVO>> page(@RequestHeader("X-User-Role") Integer role,
                                 @RequestParam(defaultValue = "1") @Min(1) int pageNum,
                                 @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
                                 @RequestParam(required = false) Integer status) {
        requireMerchant(role);
        return R.ok(orderService.merchantPage(pageNum, pageSize, status));
    }

    @GetMapping("/{orderNo}")
    public R<MerchantOrderDetailVO> detail(@RequestHeader("X-User-Role") Integer role, @PathVariable String orderNo) {
        requireMerchant(role);
        return R.ok(orderService.merchantDetail(orderNo));
    }

    @PutMapping("/{orderNo}/note")
    public R<Void> updateNote(@RequestHeader("X-User-Role") Integer role,
                              @RequestHeader("X-User-Id") Long operatorId,
                              @PathVariable String orderNo,
                              @Valid @RequestBody MerchantOrderNoteDTO dto) {
        requireMerchant(role);
        orderService.updateMerchantNote(operatorId, orderNo, dto.getNote());
        return R.ok();
    }

    @PostMapping("/{orderNo}/close")
    public R<Void> close(@RequestHeader("X-User-Role") Integer role,
                         @RequestHeader("X-User-Id") Long operatorId,
                         @PathVariable String orderNo,
                         @Valid @RequestBody MerchantCloseOrderDTO dto) {
        requireMerchant(role);
        orderService.close(operatorId, orderNo, dto.getReason());
        return R.ok();
    }

    @PostMapping("/{orderNo}/confirm-payment")
    public R<Void> confirmPayment(@RequestHeader("X-User-Role") Integer role,
                                  @RequestHeader("X-User-Id") Long operatorId,
                                  @PathVariable String orderNo) {
        requireMerchant(role);
        orderService.confirmPayment(operatorId, orderNo);
        return R.ok();
    }

    @PostMapping("/{orderNo}/accept")
    public R<Void> accept(@RequestHeader("X-User-Role") Integer role,
                          @RequestHeader("X-User-Id") Long operatorId,
                          @PathVariable String orderNo) {
        requireMerchant(role);
        orderService.accept(operatorId, orderNo);
        return R.ok();
    }

    @PostMapping("/{orderNo}/ship")
    public R<Void> ship(@RequestHeader("X-User-Role") Integer role,
                        @RequestHeader("X-User-Id") Long operatorId,
                        @PathVariable String orderNo,
                        @Valid @RequestBody MerchantShipmentDTO dto) {
        requireMerchant(role);
        orderService.ship(operatorId, orderNo, dto);
        return R.ok();
    }

    @GetMapping("/dashboard")
    public R<MerchantDashboardVO> dashboard(@RequestHeader("X-User-Role") Integer role) {
        requireMerchant(role);
        return R.ok(orderService.merchantDashboard());
    }

    private void requireMerchant(Integer role) {
        if (!Integer.valueOf(UserRole.OWNER).equals(role) && !Integer.valueOf(UserRole.STAFF).equals(role)) {
            throw new BizException(403, "仅店主或店员可处理订单");
        }
    }
}
