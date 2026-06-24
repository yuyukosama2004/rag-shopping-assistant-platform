package com.biyesheji.user.controller;

import com.biyesheji.dto.LoginDTO;
import com.biyesheji.dto.R;
import com.biyesheji.dto.RegisterDTO;
import com.biyesheji.entity.User;
import com.biyesheji.user.service.UserService;
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
        User user = userService.register(dto);
        user.setPassword(null);
        return R.ok(user);
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        LoginVO vo = userService.login(dto.getUsername(), dto.getPassword());
        return R.ok(vo);
    }

    @Operation(summary = "刷新Token")
    @PostMapping("/refresh")
    public R<LoginVO> refresh(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        LoginVO vo = userService.refreshToken(token);
        return R.ok(vo);
    }

    @Operation(summary = "获取用户信息")
    @GetMapping("/info")
    public R<User> info(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserId(token);
        User user = userService.getById(userId);
        return R.ok(user);
    }

    @Operation(summary = "修改用户信息")
    @PutMapping("/info")
    public R<User> updateInfo(@RequestHeader("Authorization") String authHeader,
                               @RequestBody User updateUser) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserId(token);
        User user = userService.updateInfo(userId, updateUser);
        return R.ok(user);
    }
}
