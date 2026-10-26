package com.skalez.coupon.api;

import com.skalez.cart.api.CartDtos.CartResponse;
import com.skalez.cart.service.CartService;
import com.skalez.coupon.api.CouponDtos.CouponPreviewRequest;
import com.skalez.coupon.api.CouponDtos.CouponPreviewResponse;
import com.skalez.coupon.domain.Coupon;
import com.skalez.coupon.service.CouponService;
import com.skalez.global.error.BusinessException;
import com.skalez.global.error.ErrorCode;
import com.skalez.global.money.Money;
import com.skalez.global.security.LoginMember;
import com.skalez.shipping.service.ShippingFeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Locale;

@RestController
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;
    private final CartService cartService;
    private final ShippingFeeService shippingFeeService;
    @PostMapping("/api/coupons/preview")
    public CouponPreviewResponse preview(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CouponPreviewRequest request, Locale locale) {
        Long memberId = LoginMember.id(jwt);
        CartResponse cart = cartService.getCart(memberId, locale);
        if (cart.items().isEmpty()) {
            throw new BusinessException(ErrorCode.CART_EMPTY);
        }
        Money items = Money.of(cart.itemsAmount(), cart.currency());
        Coupon coupon = couponService.findUsable(memberId, request.code());
        Money discount = coupon.calculateDiscount(items, Instant.now());
        Money afterDiscount = items.minus(discount);
        Money shipping = shippingFeeService.estimate(afterDiscount);
        return new CouponPreviewResponse(coupon.getCode(), coupon.getName(), cart.currency(),
                items.getAmount(), discount.getAmount(), shipping.getAmount(), afterDiscount.plus(shipping).getAmount());
    }
}