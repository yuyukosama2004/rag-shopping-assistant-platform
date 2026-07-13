package com.biyesheji.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StoreSettingUpdateDTO {

    @NotBlank(message = "店铺名称不能为空")
    @Size(max = 100, message = "店铺名称不能超过100个字符")
    private String storeName;

    @Size(max = 255, message = "Logo地址不能超过255个字符")
    private String logo;

    @Size(max = 20, message = "联系电话不能超过20个字符")
    private String servicePhone;

    @Size(max = 100, message = "联系邮箱不能超过100个字符")
    private String serviceEmail;

    @Size(max = 255, message = "店铺地址不能超过255个字符")
    private String address;

    @Min(value = 0, message = "营业状态只能为0或1")
    @Max(value = 1, message = "营业状态只能为0或1")
    private Integer businessStatus;

    @Size(max = 2000, message = "配送说明不能超过2000个字符")
    private String shippingNotice;

    @Size(max = 2000, message = "售后说明不能超过2000个字符")
    private String afterSalesNotice;
}
