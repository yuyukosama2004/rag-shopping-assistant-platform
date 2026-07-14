package com.biyesheji.order.controller;

import com.biyesheji.dto.CartAddDTO;
import com.biyesheji.dto.CartBatchDeleteDTO;
import com.biyesheji.dto.CartCheckAllDTO;
import com.biyesheji.dto.CartOptionsDTO;
import com.biyesheji.dto.CartQuantityDTO;
import com.biyesheji.dto.R;
import com.biyesheji.entity.ShoppingCart;
import com.biyesheji.order.service.ShoppingCartService;
import com.biyesheji.utils.JwtUtil;
import com.biyesheji.vo.CartItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "购物车接口")
@RestController
@RequestMapping("/api/order/cart")
@RequiredArgsConstructor
public class CartController {

    private final ShoppingCartService cartService;
    private final JwtUtil jwtUtil;

    private Long getUserId(String auth) {
        return jwtUtil.getAccessUserId(auth.replace("Bearer ", ""));
    }

    @Operation(summary = "加入购物车")
    @PostMapping
    public R<ShoppingCart> add(@RequestHeader("Authorization") String auth,
                               @Valid @RequestBody CartAddDTO body) {
        Long userId = getUserId(auth);
        ShoppingCart cart = cartService.add(userId, body.getProductId(), body.getSkuId(), body.getQuantity());
        if (body.getColor() != null || body.getStorage() != null) {
            cartService.updateOptions(userId, cart.getId(), body.getColor(), body.getStorage());
        }
        return R.ok(cart);
    }

    @Operation(summary = "修改外观和规格")
    @PutMapping("/{cartId}/options")
    public R<Void> updateOptions(@RequestHeader("Authorization") String auth,
                                 @PathVariable Long cartId,
                                 @Valid @RequestBody CartOptionsDTO body) {
        cartService.updateOptions(getUserId(auth), cartId, body.getColor(), body.getStorage());
        return R.ok();
    }

    @Operation(summary = "购物车列表")
    @GetMapping
    public R<List<CartItemVO>> list(@RequestHeader("Authorization") String auth) {
        return R.ok(cartService.list(getUserId(auth)));
    }

    @Operation(summary = "修改数量")
    @PutMapping("/{cartId}")
    public R<ShoppingCart> updateQuantity(@RequestHeader("Authorization") String auth,
                                          @PathVariable Long cartId,
                                          @Valid @RequestBody CartQuantityDTO body) {
        return R.ok(cartService.updateQuantity(getUserId(auth), cartId, body.getQuantity()));
    }

    @Operation(summary = "删除购物车项")
    @DeleteMapping("/{cartId}")
    public R<Void> remove(@RequestHeader("Authorization") String auth,
                          @PathVariable Long cartId) {
        cartService.remove(getUserId(auth), cartId);
        return R.ok();
    }

    @Operation(summary = "批量删除")
    @DeleteMapping("/batch")
    public R<Void> removeBatch(@RequestHeader("Authorization") String auth,
                               @Valid @RequestBody CartBatchDeleteDTO body) {
        cartService.removeBatch(getUserId(auth), body.getIds());
        return R.ok();
    }

    @Operation(summary = "切换选中状态")
    @PutMapping("/{cartId}/check")
    public R<Void> toggleCheck(@RequestHeader("Authorization") String auth,
                               @PathVariable Long cartId) {
        cartService.toggleCheck(getUserId(auth), cartId);
        return R.ok();
    }

    @Operation(summary = "全选或取消全选")
    @PutMapping("/check-all")
    public R<Void> checkAll(@RequestHeader("Authorization") String auth,
                            @Valid @RequestBody CartCheckAllDTO body) {
        cartService.checkAll(getUserId(auth), body.getChecked());
        return R.ok();
    }

    @Operation(summary = "购物车数量")
    @GetMapping("/count")
    public R<Integer> count(@RequestHeader("Authorization") String auth) {
        return R.ok(cartService.count(getUserId(auth)));
    }
}
