package com.biyesheji.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CartCheckAllDTO {
    @NotNull(message = "选中状态不能为空")
    private Boolean checked;
}
