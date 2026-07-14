package com.biyesheji.order.service;

import com.biyesheji.entity.ShoppingCart;
import com.biyesheji.vo.CartItemVO;

import java.util.List;

public interface ShoppingCartService {
    ShoppingCart add(Long userId, Long productId, Long skuId, Integer quantity);
    ShoppingCart updateQuantity(Long userId, Long cartId, Integer quantity);
    void remove(Long userId, Long cartId);
    void removeBatch(Long userId, List<Long> cartIds);
    void toggleCheck(Long userId, Long cartId);
    void checkAll(Long userId, boolean checked);
    List<CartItemVO> list(Long userId);
    int count(Long userId);
    void updateOptions(Long userId, Long cartId, String color, String storage);
}
