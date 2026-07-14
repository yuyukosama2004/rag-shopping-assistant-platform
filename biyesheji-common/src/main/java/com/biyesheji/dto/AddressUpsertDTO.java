package com.biyesheji.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddressUpsertDTO {
    @NotBlank(message = "收货人不能为空")
    @Size(max = 50, message = "收货人长度不能超过50")
    private String receiverName;

    @NotBlank(message = "收货电话不能为空")
    @Size(max = 20, message = "收货电话长度不能超过20")
    private String receiverPhone;

    @Size(max = 50, message = "省份长度不能超过50")
    private String province;

    @Size(max = 50, message = "城市长度不能超过50")
    private String city;

    @Size(max = 50, message = "区县长度不能超过50")
    private String district;

    @NotBlank(message = "详细地址不能为空")
    @Size(max = 255, message = "详细地址长度不能超过255")
    private String detail;
}
