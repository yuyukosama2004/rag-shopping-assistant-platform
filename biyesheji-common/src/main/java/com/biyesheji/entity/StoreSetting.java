package com.biyesheji.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_store_setting")
public class StoreSetting extends BaseEntity {

    public static final long SINGLE_STORE_ID = 1L;

    @TableId(type = IdType.INPUT)
    private Long id;

    private String storeName;
    private String logo;
    private String servicePhone;
    private String serviceEmail;
    private String address;
    private Integer businessStatus;
    private String shippingNotice;
    private String afterSalesNotice;
}
