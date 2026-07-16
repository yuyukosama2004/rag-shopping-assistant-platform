package com.biyesheji.user.controller;

import com.biyesheji.dto.LoginDTO;
import com.biyesheji.dto.R;
import com.biyesheji.dto.RegisterDTO;
import com.biyesheji.dto.UserUpdateDTO;
import com.biyesheji.entity.User;
import com.biyesheji.exception.BizException;
import com.biyesheji.user.service.UserService;
import com.biyesheji.user.dto.AccountDeleteDTO;
import com.biyesheji.user.vo.AccountDataExportVO;
import com.biyesheji.utils.JwtUtil;
import com.biyesheji.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户接口", description = "注册、登录、用户信息")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @Operation(summary = "注册")
    @PostMapping("/register")
    public R<User> register(@Valid @RequestBody RegisterDTO dto) {
        return R.ok(userService.register(dto));
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return R.ok(userService.login(dto.getUsername(), dto.getPassword()));
    }

    @Operation(summary = "刷新令牌")
    @PostMapping("/refresh")
    public R<LoginVO> refresh(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        if (!JwtUtil.REFRESH_TOKEN.equals(jwtUtil.getTokenType(token))) {
            throw new BizException(401, "仅允许使用刷新令牌");
        }
        return R.ok(userService.refreshToken(token));
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public R<Void> logout(@RequestHeader("Authorization") String authHeader) {
        userService.logout(jwtUtil.getAccessUserId(authHeader.replace("Bearer ", "")));
        return R.ok();
    }

    @Operation(summary = "获取用户信息")
    @GetMapping("/info")
    public R<User> info(@RequestHeader("Authorization") String authHeader) {
        return R.ok(userService.getById(jwtUtil.getAccessUserId(authHeader.replace("Bearer ", ""))));
    }

    @Operation(summary = "修改用户信息")
    @PutMapping("/info")
    public R<User> updateInfo(@RequestHeader("Authorization") String authHeader,
                              @Valid @RequestBody UserUpdateDTO body) {
        User updateUser = new User();
        updateUser.setNickname(body.getNickname());
        updateUser.setPhone(body.getPhone());
        updateUser.setEmail(body.getEmail());
        updateUser.setAvatar(body.getAvatar());
        return R.ok(userService.updateInfo(jwtUtil.getAccessUserId(authHeader.replace("Bearer ", "")), updateUser));
    }

    @Operation(summary = "导出当前消费者的账户资料")
    @GetMapping("/export")
    public R<AccountDataExportVO> export(@RequestHeader("Authorization") String authHeader) {
        return R.ok(userService.exportAccountData(jwtUtil.getAccessUserId(authHeader.replace("Bearer ", ""))));
    }

    @Operation(summary = "注销当前消费者账户")
    @DeleteMapping("/account")
    public R<Void> deleteAccount(@RequestHeader("Authorization") String authHeader,
                                 @Valid @RequestBody AccountDeleteDTO dto) {
        userService.deleteAccount(jwtUtil.getAccessUserId(authHeader.replace("Bearer ", "")), dto.getPassword(), dto.getConfirmation());
        return R.ok();
    }
}
