package com.biyesheji.user.controller;

import com.biyesheji.dto.R;
import com.biyesheji.dto.AddressUpsertDTO;
import com.biyesheji.entity.Address;
import com.biyesheji.user.service.AddressService;
import com.biyesheji.utils.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "地址接口", description = "收货地址管理")
@RestController
@RequestMapping("/api/user/address")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;
    private final JwtUtil jwtUtil;

    private Long getUserId(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtUtil.getAccessUserId(token);
    }

    @Operation(summary = "获取地址列表")
    @GetMapping
    public R<List<Address>> list(@RequestHeader("Authorization") String authHeader) {
        return R.ok(addressService.listByUserId(getUserId(authHeader)));
    }

    @Operation(summary = "获取地址详情")
    @GetMapping("/{id}")
    public R<Address> getById(@RequestHeader("Authorization") String authHeader,
                              @PathVariable Long id) {
        Address address = addressService.getById(id);
        if (address == null || !address.getUserId().equals(getUserId(authHeader))) {
            throw new com.biyesheji.exception.BizException(404, "地址不存在");
        }
        return R.ok(address);
    }

    @Operation(summary = "新增地址")
    @PostMapping
    public R<Address> add(@RequestHeader("Authorization") String authHeader,
                           @Valid @RequestBody AddressUpsertDTO body) {
        return R.ok(addressService.add(getUserId(authHeader), toAddress(body)));
    }

    @Operation(summary = "修改地址")
    @PutMapping("/{id}")
    public R<Address> update(@RequestHeader("Authorization") String authHeader,
                              @PathVariable Long id, @Valid @RequestBody AddressUpsertDTO body) {
        Address address = toAddress(body);
        address.setId(id);

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

    private Address toAddress(AddressUpsertDTO body) {
        Address address = new Address();
        address.setReceiverName(body.getReceiverName());
        address.setReceiverPhone(body.getReceiverPhone());
        address.setProvince(body.getProvince());
        address.setCity(body.getCity());
        address.setDistrict(body.getDistrict());
        address.setDetail(body.getDetail());
        return address;
    }
}
