package com.biyesheji.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.biyesheji.entity.Order;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AccountOrderMapper extends BaseMapper<Order> {
}
