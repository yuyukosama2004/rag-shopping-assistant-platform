package com.biyesheji.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MerchantShipmentDTO {
    @NotBlank(message = "承运商不能为空")
    @Size(max = 64, message = "承运商长度不能超过64")
    private String carrier;

    @NotBlank(message = "运单号不能为空")
    @Size(max = 64, message = "运单号长度不能超过64")
    private String trackingNo;

    @Size(max = 255, message = "发货备注长度不能超过255")
    private String note;
}
