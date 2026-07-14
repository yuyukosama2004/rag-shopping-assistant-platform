package com.biyesheji.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StaffPasswordResetDTO {
    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 72, message = "密码长度应为8到72个字符")
    private String newPassword;
}
