package com.biyesheji.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.biyesheji.entity.ShoppingCart;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AccountShoppingCartMapper extends BaseMapper<ShoppingCart> {
}
