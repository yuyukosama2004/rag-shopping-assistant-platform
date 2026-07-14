package com.biyesheji.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.biyesheji.entity.Product;
import com.biyesheji.entity.ProductSku;
import com.biyesheji.entity.ShoppingCart;
import com.biyesheji.exception.BizException;
import com.biyesheji.order.mapper.ShoppingCartMapper;
import com.biyesheji.order.service.ShoppingCartService;
import com.biyesheji.order.mapper.ProductMapper;
import com.biyesheji.order.mapper.ProductSkuMapper;
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
    private final ProductSkuMapper productSkuMapper;

    @Override
    public ShoppingCart add(Long userId, Long productId, Long skuId, Integer quantity) {
        validateQuantity(quantity);
        Product product = productMapper.selectById(productId);
        if (product == null || product.getStatus() == 0) {
            throw new BizException("商品不存在或已下架");
        }
        ProductSku sku = productSkuMapper.selectById(skuId);
        if (sku == null || sku.getStatus() != 1 || !productId.equals(sku.getProductId())) {
            throw new BizException("SKU不存在、已停用或不属于该商品");
        }

        // 检查是否已在购物车
        ShoppingCart existing = cartMapper.selectOne(
                new LambdaQueryWrapper<ShoppingCart>()
                        .eq(ShoppingCart::getUserId, userId)
                        .eq(ShoppingCart::getSkuId, skuId)
        );

        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + quantity);
            cartMapper.updateById(existing);
            return existing;
        }

        ShoppingCart cart = new ShoppingCart();
        cart.setUserId(userId);
        cart.setProductId(productId);
        cart.setSkuId(skuId);
        cart.setQuantity(quantity);
        cart.setChecked(1);
        cartMapper.insert(cart);
        return cart;
    }

    @Override
    public ShoppingCart updateQuantity(Long userId, Long cartId, Integer quantity) {
        validateQuantity(quantity);
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
            ProductSku sku = productSkuMapper.selectById(cart.getSkuId());
            return CartItemVO.from(cart, product, sku);
        }).collect(Collectors.toList());
    }

    @Override
    public void updateOptions(Long userId, Long cartId, String color, String storage) {
        ShoppingCart cart = cartMapper.selectById(cartId);
        if (cart == null || !cart.getUserId().equals(userId)) {
            throw new BizException("购物车项不存在");
        }
        if (color != null) cart.setSelectedColor(color);
        if (storage != null) cart.setSelectedStorage(storage);
        cartMapper.updateById(cart);
    }

    @Override
    public int count(Long userId) {
        Long cnt = cartMapper.selectCount(
                new LambdaQueryWrapper<ShoppingCart>().eq(ShoppingCart::getUserId, userId)
        );
        return cnt.intValue();
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BizException("商品数量必须大于0");
        }
    }
}
