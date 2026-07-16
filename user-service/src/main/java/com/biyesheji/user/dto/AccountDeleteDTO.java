package com.biyesheji.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AccountDeleteDTO {
    @NotBlank(message = "请输入当前密码")
    @Size(max = 100, message = "密码长度不正确")
    private String password;

    @NotBlank(message = "缺少注销确认")
    private String confirmation;
}
