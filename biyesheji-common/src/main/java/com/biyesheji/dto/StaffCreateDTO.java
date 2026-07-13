package com.biyesheji.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StaffCreateDTO {
    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名不能超过50个字符")
    private String username;
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 72, message = "密码长度应为8到72个字符")
    private String password;
    @Size(max = 50, message = "昵称不能超过50个字符")
    private String nickname;
    @Size(max = 20, message = "手机号不能超过20个字符")
    private String phone;
    @Size(max = 100, message = "邮箱不能超过100个字符")
    private String email;
}
