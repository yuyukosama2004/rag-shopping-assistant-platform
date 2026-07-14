package com.biyesheji.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biyesheji.dto.OrderSubmitDTO;
import com.biyesheji.dto.R;
import com.biyesheji.order.service.OrderService;
import com.biyesheji.utils.JwtUtil;
import com.biyesheji.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

@Tag(name = "订单接口", description = "下单、查询、支付、取消")
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
@Validated
public class OrderController {

    private final OrderService orderService;
    private final JwtUtil jwtUtil;

    private Long getUserId(String auth) {
        return jwtUtil.getAccessUserId(auth.replace("Bearer ", ""));
    }

    @Operation(summary = "提交订单")
    @PostMapping("/submit")
    public R<Map<String, String>> submit(@RequestHeader("Authorization") String auth,
                                          @Valid @RequestBody OrderSubmitDTO dto) {
        String orderNo = orderService.submit(getUserId(auth), dto);
        return R.ok(Map.of("orderNo", orderNo, "status", "processing"));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{orderNo}")
    public R<OrderVO> detail(@RequestHeader("Authorization") String auth,
                             @PathVariable String orderNo) {
        return R.ok(orderService.detail(getUserId(auth), orderNo));
    }

    @Operation(summary = "订单列表")
    @GetMapping("/page")
    public R<Page<OrderVO>> page(@RequestHeader("Authorization") String auth,
                                 @RequestParam(defaultValue = "1") @Min(1) int pageNum,
                                 @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize,
                                 @RequestParam(required = false) Integer status) {
        return R.ok(orderService.page(getUserId(auth), pageNum, pageSize, status));
    }

    @Operation(summary = "模拟支付")
    @PostMapping("/{orderNo}/pay")
    public R<Void> pay(@RequestHeader("Authorization") String auth,
                       @PathVariable String orderNo) {
        orderService.pay(getUserId(auth), orderNo);
        return R.ok();
    }

    @Operation(summary = "取消订单")
    @PostMapping("/{orderNo}/cancel")
    public R<Void> cancel(@RequestHeader("Authorization") String auth,
                          @PathVariable String orderNo) {
        orderService.cancel(getUserId(auth), orderNo);
        return R.ok();
    }

    @Operation(summary = "确认收货")
    @PostMapping("/{orderNo}/complete")
    public R<Void> complete(@RequestHeader("Authorization") String auth,
                            @PathVariable String orderNo) {
        orderService.complete(getUserId(auth), orderNo);
        return R.ok();
    }
}
