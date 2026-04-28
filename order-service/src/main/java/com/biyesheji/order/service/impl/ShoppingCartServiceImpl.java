package com.biyesheji.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.biyesheji.entity.Product;
import com.biyesheji.entity.ShoppingCart;
import com.biyesheji.exception.BizException;
import com.biyesheji.order.mapper.ShoppingCartMapper;
import com.biyesheji.order.service.ShoppingCartService;
import com.biyesheji.order.mapper.ProductMapper;
import com.biyesheji.vo.CartItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShoppingCartServiceImpl implements ShoppingCartService {

    private final ShoppingCartMapper cartMapper;
    private final ProductMapper productMapper;

    @Override
    public ShoppingCart add(Long userId, Long productId, Integer quantity) {
        Product product = productMapper.selectById(productId);
        if (product == null || product.getStatus() == 0) {
            throw new BizException("商品不存在或已下架");
        }

        // 检查是否已在购物车
        ShoppingCart existing = cartMapper.selectOne(
                new LambdaQueryWrapper<ShoppingCart>()
                        .eq(ShoppingCart::getUserId, userId)
                        .eq(ShoppingCart::getProductId, productId)
        );

        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + quantity);
            cartMapper.updateById(existing);
            return existing;
        }

        ShoppingCart cart = new ShoppingCart();
        cart.setUserId(userId);
        cart.setProductId(productId);
        cart.setQuantity(quantity);
        cart.setChecked(1);
        cartMapper.insert(cart);
        return cart;
    }

    @Override
    public ShoppingCart updateQuantity(Long userId, Long cartId, Integer quantity) {
        ShoppingCart cart = cartMapper.selectById(cartId);
        if (cart == null || !cart.getUserId().equals(userId)) {
            throw new BizException("购物车项不存在");
        }
        cart.setQuantity(quantity);
        cartMapper.updateById(cart);
        return cart;
    }

    @Override
    public void remove(Long userId, Long cartId) {
        ShoppingCart cart = cartMapper.selectById(cartId);
        if (cart != null && cart.getUserId().equals(userId)) {
            cartMapper.deleteById(cartId);
        }
    }

    @Override
    public void removeBatch(Long userId, List<Long> cartIds) {
        for (Long id : cartIds) {
            remove(userId, id);
        }
    }

    @Override
    public void toggleCheck(Long userId, Long cartId) {
        ShoppingCart cart = cartMapper.selectById(cartId);
        if (cart != null && cart.getUserId().equals(userId)) {
            cart.setChecked(cart.getChecked() == 1 ? 0 : 1);
            cartMapper.updateById(cart);
        }
    }

    @Override
    public void checkAll(Long userId, boolean checked) {
        cartMapper.update(null, new LambdaUpdateWrapper<ShoppingCart>()
                .eq(ShoppingCart::getUserId, userId)
                .set(ShoppingCart::getChecked, checked ? 1 : 0));
    }

    @Override
    public List<CartItemVO> list(Long userId) {
        List<ShoppingCart> carts = cartMapper.selectList(
                new LambdaQueryWrapper<ShoppingCart>().eq(ShoppingCart::getUserId, userId)
        );
        return carts.stream().map(cart -> {
            Product product = productMapper.selectById(cart.getProductId());
            return CartItemVO.from(cart, product);
        }).collect(Collectors.toList());
    }

    @Override
    public int count(Long userId) {
        Long cnt = cartMapper.selectCount(
                new LambdaQueryWrapper<ShoppingCart>().eq(ShoppingCart::getUserId, userId)
        );
        return cnt.intValue();
    }
}
