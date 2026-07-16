package com.biyesheji.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.biyesheji.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AccountOrderItemMapper extends BaseMapper<OrderItem> {
}
