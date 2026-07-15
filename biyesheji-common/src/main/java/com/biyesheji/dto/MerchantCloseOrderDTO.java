package com.biyesheji.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MerchantCloseOrderDTO {
    @NotBlank(message = "请填写关单原因")
    @Size(max = 255, message = "关单原因不能超过255个字符")
    private String reason;
}
