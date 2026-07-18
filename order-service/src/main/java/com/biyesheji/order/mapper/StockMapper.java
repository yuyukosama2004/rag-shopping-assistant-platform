package com.biyesheji.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.biyesheji.entity.Stock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface StockMapper extends BaseMapper<Stock> {
    @Select("SELECT * FROM t_stock WHERE sku_id = #{skuId} FOR UPDATE")
    Stock selectBySkuIdForUpdate(@Param("skuId") Long skuId);
}
