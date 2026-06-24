package com.biyesheji.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("t_stock")
public class Stock implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;
    private Integer total;
    private Integer locked;
    private Integer available;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
