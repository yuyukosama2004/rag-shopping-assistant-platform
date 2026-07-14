package com.biyesheji.order.controller;

import com.biyesheji.dto.R;
import com.biyesheji.dto.RefundRequestDTO;
import com.biyesheji.entity.RefundRecord;
import com.biyesheji.order.service.RefundService;
import com.biyesheji.utils.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/order/{orderNo}/refunds")
@RequiredArgsConstructor
public class RefundController {
    private final RefundService service;
    private final JwtUtil jwtUtil;

    private Long userId(String auth) { return jwtUtil.getAccessUserId(auth.replace("Bearer ", "")); }

    @GetMapping
    public R<List<RefundRecord>> list(@RequestHeader("Authorization") String auth, @PathVariable String orderNo) {
        return R.ok(service.listForCustomer(userId(auth), orderNo));
    }

    @PostMapping
    public R<RefundRecord> request(@RequestHeader("Authorization") String auth, @PathVariable String orderNo, @Valid @RequestBody RefundRequestDTO dto) {
        return R.ok(service.request(userId(auth), orderNo, dto));
    }
}
