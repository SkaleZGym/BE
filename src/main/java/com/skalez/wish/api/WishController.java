package com.skalez.wish.api;

import com.skalez.global.security.LoginMember;
import com.skalez.product.api.ProductResponse;
import com.skalez.wish.service.WishService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/wishes")
@RequiredArgsConstructor
public class WishController {
    private final WishService wishService;
    @GetMapping
    public List<ProductResponse> getWishes(@AuthenticationPrincipal Jwt jwt, Locale locale){
        return wishService.getWishes(LoginMember.id(jwt), locale);
    }
    @PutMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addWish(@AuthenticationPrincipal Jwt jwt, @PathVariable Long productId){
        wishService.addWish(LoginMember.id(jwt), productId);
    }
    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeWish(@AuthenticationPrincipal Jwt jwt, @PathVariable Long productId){
        wishService.removeWish(LoginMember.id(jwt), productId);
    }
}
