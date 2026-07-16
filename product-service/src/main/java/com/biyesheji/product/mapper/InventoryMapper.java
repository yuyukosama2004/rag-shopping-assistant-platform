package com.biyesheji.product.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biyesheji.vo.MerchantInventoryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface InventoryMapper {
    @Select("""
            <script>
            SELECT p.id AS product_id, p.name AS product_name, p.status AS product_status,
                   sku.id AS sku_id, sku.sku_code, sku.spec_json, sku.status AS sku_status,
                   stock.total, stock.locked, stock.available, stock.updated_at
            FROM t_product_sku sku
            JOIN t_product p ON p.id = sku.product_id
            JOIN t_stock stock ON stock.sku_id = sku.id
            WHERE p.deleted = 0 AND p.status != 3
            <if test="keyword != null and keyword != ''">
              AND (p.name LIKE CONCAT('%', #{keyword}, '%') OR sku.sku_code LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="lowStockOnly">
              AND p.status = 1 AND sku.status = 1 AND stock.available &lt;= #{threshold}
            </if>
            ORDER BY stock.available ASC, stock.updated_at DESC
            </script>
            """)
    Page<MerchantInventoryVO> selectInventoryPage(Page<MerchantInventoryVO> page,
                                                    @Param("keyword") String keyword,
                                                    @Param("lowStockOnly") boolean lowStockOnly,
                                                    @Param("threshold") int threshold);

    @Select("""
            SELECT COUNT(*)
            FROM t_product_sku sku
            JOIN t_product p ON p.id = sku.product_id
            JOIN t_stock stock ON stock.sku_id = sku.id
            WHERE p.deleted = 0 AND p.status = 1 AND sku.status = 1 AND stock.available <= #{threshold}
            """)
    int countLowStock(@Param("threshold") int threshold);
}
