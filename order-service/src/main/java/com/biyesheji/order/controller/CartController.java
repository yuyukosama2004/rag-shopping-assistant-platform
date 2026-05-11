package com.biyesheji.order.controller;

import com.biyesheji.dto.R;
import com.biyesheji.entity.ShoppingCart;
import com.biyesheji.order.service.ShoppingCartService;
import com.biyesheji.utils.JwtUtil;
import com.biyesheji.vo.CartItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "购物车接口")
@RestController
@RequestMapping("/api/order/cart")
@RequiredArgsConstructor
public class CartController {

    private final ShoppingCartService cartService;
    private final JwtUtil jwtUtil;

    private Long getUserId(String auth) {
        return jwtUtil.getUserId(auth.replace("Bearer ", ""));
    }

    @Operation(summary = "加入购物车")
    @PostMapping
    public R<ShoppingCart> add(@RequestHeader("Authorization") String auth,
                                @RequestBody Map<String, Object> body) {
        Long productId = Long.valueOf(body.get("productId").toString());
        Integer quantity = body.containsKey("quantity")
                ? Integer.valueOf(body.get("quantity").toString()) : 1;
        ShoppingCart cart = cartService.add(getUserId(auth), productId, quantity);
        if (body.containsKey("color")) cart.setSelectedColor(body.get("color").toString());
        if (body.containsKey("storage")) cart.setSelectedStorage(body.get("storage").toString());
        if (body.containsKey("color") || body.containsKey("storage")) {
            cartService.updateOptions(cart.getId(), cart.getSelectedColor(), cart.getSelectedStorage());
        }
        return R.ok(cart);
    }

    @Operation(summary = "修改外观/规格")
    @PutMapping("/{cartId}/options")
    public R<Void> updateOptions(@RequestHeader("Authorization") String auth,
                                  @PathVariable Long cartId,
                                  @RequestBody Map<String, String> body) {
        cartService.updateOptions(cartId, body.get("color"), body.get("storage"));
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
                                           @RequestBody Map<String, Integer> body) {
        return R.ok(cartService.updateQuantity(getUserId(auth), cartId, body.get("quantity")));
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
                                @RequestBody Map<String, List<Long>> body) {
        cartService.removeBatch(getUserId(auth), body.get("ids"));
        return R.ok();
    }

    @Operation(summary = "切换选中状态")
    @PutMapping("/{cartId}/check")
    public R<Void> toggleCheck(@RequestHeader("Authorization") String auth,
                                @PathVariable Long cartId) {
        cartService.toggleCheck(getUserId(auth), cartId);
        return R.ok();
    }

    @Operation(summary = "全选/取消全选")
    @PutMapping("/check-all")
    public R<Void> checkAll(@RequestHeader("Authorization") String auth,
                             @RequestBody Map<String, Boolean> body) {
        cartService.checkAll(getUserId(auth), body.get("checked"));
        return R.ok();
    }

    @Operation(summary = "购物车数量")
    @GetMapping("/count")
    public R<Integer> count(@RequestHeader("Authorization") String auth) {
        return R.ok(cartService.count(getUserId(auth)));
    }
}
