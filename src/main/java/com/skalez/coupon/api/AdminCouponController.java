package com.skalez.coupon.api;

import com.skalez.coupon.api.CouponDtos.AdminCouponResponse;
import com.skalez.coupon.api.CouponDtos.CouponSaveRequest;
import com.skalez.coupon.service.CouponService;
import com.skalez.global.web.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/coupons")
@RequiredArgsConstructor
public class AdminCouponController {
    private final CouponService couponService;
    @GetMapping
    public PageResponse<AdminCouponResponse> getCoupons(@PageableDefault(size=  20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable){
        return couponService.getCoupons(pageable);
    }
    @GetMapping("/{couponId}")
    public AdminCouponResponse getCoupon(@PathVariable Long couponId){
        return couponService.getCoupon(couponId);
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminCouponResponse create(@Valid @RequestBody CouponSaveRequest request){
        return couponService.create(request);
    }
    @PutMapping("/{couponId}")
    public AdminCouponResponse update(@PathVariable Long couponId, @Valid @RequestBody CouponSaveRequest request){
        return couponService.update(couponId, request);
    }
}
