package com.biyesheji.user.controller;

import com.biyesheji.dto.R;
import com.biyesheji.entity.Address;
import com.biyesheji.user.service.AddressService;
import com.biyesheji.utils.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "地址接口", description = "收货地址管理")
@RestController
@RequestMapping("/api/user/address")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;
    private final JwtUtil jwtUtil;

    private Long getUserId(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtUtil.getUserId(token);
    }

    @Operation(summary = "获取地址列表")
    @GetMapping
    public R<List<Address>> list(@RequestHeader("Authorization") String authHeader) {
        return R.ok(addressService.listByUserId(getUserId(authHeader)));
    }

    @Operation(summary = "获取地址详情")
    @GetMapping("/{id}")
    public R<Address> getById(@PathVariable Long id) {
        return R.ok(addressService.getById(id));
    }

    @Operation(summary = "新增地址")
    @PostMapping
    public R<Address> add(@RequestHeader("Authorization") String authHeader,
                           @RequestBody Address address) {
        return R.ok(addressService.add(getUserId(authHeader), address));
    }

    @Operation(summary = "修改地址")
    @PutMapping("/{id}")
    public R<Address> update(@RequestHeader("Authorization") String authHeader,
                              @PathVariable Long id, @RequestBody Address address) {
        log.info("PUT /address/{} body: name={} phone={} detail={}", id, address.getReceiverName(), address.getReceiverPhone(), address.getDetail());
        address.setId(id);
        log.info("After setId: address.id={}", address.getId());

        return R.ok(addressService.update(getUserId(authHeader), address));
    }

    @Operation(summary = "删除地址")
    @DeleteMapping("/{id}")
    public R<Void> delete(@RequestHeader("Authorization") String authHeader,
                           @PathVariable Long id) {
        addressService.delete(getUserId(authHeader), id);
        return R.ok();
    }

    @Operation(summary = "设为默认地址")
    @PutMapping("/{id}/default")
    public R<Void> setDefault(@RequestHeader("Authorization") String authHeader,
                               @PathVariable Long id) {
        addressService.setDefault(getUserId(authHeader), id);
        return R.ok();
    }
}
