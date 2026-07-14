package com.biyesheji.vo;

import com.biyesheji.entity.ShoppingCart;
import com.biyesheji.entity.Product;
import com.biyesheji.entity.ProductSku;
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
    private Long skuId;
    private String skuCode;
    private String skuSpecJson;
    private Integer quantity;
    private Integer checked;
    private String productName;
    private String productImage;
    private BigDecimal productPrice;
    private String selectedColor;
    private String selectedStorage;
    private String colorOptions;
    private String storageOptions;

    public static CartItemVO from(ShoppingCart cart, Product product, ProductSku sku) {
        CartItemVO vo = new CartItemVO();
        vo.setId(cart.getId());
        vo.setUserId(cart.getUserId());
        vo.setProductId(cart.getProductId());
        vo.setSkuId(cart.getSkuId());
        vo.setQuantity(cart.getQuantity());
        vo.setChecked(cart.getChecked());
        vo.setSelectedColor(cart.getSelectedColor());
        vo.setSelectedStorage(cart.getSelectedStorage());
        if (product != null) {
            vo.setProductName(product.getName());
            vo.setProductImage(product.getMainImage());
            vo.setProductPrice(sku == null ? product.getPrice() : sku.getPrice());
            vo.setColorOptions(product.getColorOptions());
            vo.setStorageOptions(product.getStorageOptions());
        }
        if (sku != null) {
            vo.setSkuCode(sku.getSkuCode());
            vo.setSkuSpecJson(sku.getSpecJson());
        }
        return vo;
    }
}
