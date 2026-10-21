package com.skalez.cart.api;

import com.skalez.cart.api.CartDtos.AddCartItemRequest;
import com.skalez.cart.api.CartDtos.CartResponse;
import com.skalez.cart.api.CartDtos.ChangeQuantityRequest;
import com.skalez.cart.service.CartService;
import com.skalez.global.security.LoginMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;
    @GetMapping
    public CartResponse getCart(@AuthenticationPrincipal Jwt jwt, Locale locale){return cartService.getCart(LoginMember.id(jwt), locale);}
    @PostMapping("/items")
    public CartResponse addItem(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AddCartItemRequest request, Locale locale){
        Long memberId = LoginMember.id(jwt);
        cartService.addItem(memberId, request);
        return cartService.getCart(memberId, locale);
    }
    @PatchMapping("/items/{cartItemId}")
    public CartResponse changeQuantity(@AuthenticationPrincipal Jwt jwt, @PathVariable Long cartItemId, @Valid @RequestBody ChangeQuantityRequest request, Locale locale){
        Long memberId = LoginMember.id(jwt);
        cartService.changeQuantity(memberId, cartItemId, request.quantity());
        return cartService.getCart(memberId, locale);
    }
    @DeleteMapping("/items/{cartItemId}")
    public CartResponse removeItem(@AuthenticationPrincipal Jwt jwt, @PathVariable Long cartItemId, Locale locale){
        Long memberId = LoginMember.id(jwt);
        cartService.removeItem(memberId, cartItemId);
        return cartService.getCart(memberId, locale);
    }
}
