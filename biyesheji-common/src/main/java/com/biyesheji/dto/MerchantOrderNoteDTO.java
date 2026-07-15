package com.biyesheji.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MerchantOrderNoteDTO {
    @Size(max = 500, message = "商家备注不能超过500个字符")
    private String note;
}
