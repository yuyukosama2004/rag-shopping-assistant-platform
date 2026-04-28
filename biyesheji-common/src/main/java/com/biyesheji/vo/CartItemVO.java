package com.biyesheji.vo;

import com.biyesheji.entity.ShoppingCart;
import com.biyesheji.entity.Product;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 购物车项视图 — 关联 ShoppingCart + Product 关键字段
 */
@Data
public class CartItemVO implements Serializable {
    private Long id;              // cart id
    private Long userId;
    private Long productId;
    private Integer quantity;
    private Integer checked;
    private String productName;
    private String productImage;
    private BigDecimal productPrice;

    public static CartItemVO from(ShoppingCart cart, Product product) {
        CartItemVO vo = new CartItemVO();
        vo.setId(cart.getId());
        vo.setUserId(cart.getUserId());
        vo.setProductId(cart.getProductId());
        vo.setQuantity(cart.getQuantity());
        vo.setChecked(cart.getChecked());
        if (product != null) {
            vo.setProductName(product.getName());
            vo.setProductImage(product.getMainImage());
            vo.setProductPrice(product.getPrice());
        }
        return vo;
    }
}
