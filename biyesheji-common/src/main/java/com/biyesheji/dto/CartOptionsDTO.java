package com.biyesheji.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CartOptionsDTO {
    @Size(max = 50, message = "颜色长度不能超过50")
    private String color;

    @Size(max = 50, message = "存储规格长度不能超过50")
    private String storage;
}
