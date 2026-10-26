package com.skalez.coupon.service;

import com.skalez.coupon.api.CouponDtos.AdminCouponResponse;
import com.skalez.coupon.api.CouponDtos.CouponSaveRequest;
import com.skalez.coupon.domain.Coupon;
import com.skalez.coupon.domain.CouponRedemption;
import com.skalez.coupon.repository.CouponRedemptionRepository;
import com.skalez.coupon.repository.CouponRepository;
import com.skalez.global.error.BusinessException;
import com.skalez.global.error.ErrorCode;
import com.skalez.global.money.Money;
import com.skalez.global.web.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponRedemptionRepository redemptionRepository;
    public Coupon findUsable(Long memberId, String code) {
        Coupon coupon = couponRepository.findByCode(Coupon.normalize(code)).orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));
        if (redemptionRepository.countByCouponIdAndMemberId(coupon.getId(), memberId) >= coupon.getPerMemberLimit()) {
            throw new BusinessException(ErrorCode.COUPON_USAGE_EXCEEDED);
        }
        return coupon;
    }
    @Transactional
    public Money redeem(Long memberId, String code, Money itemsAmount, String orderNumber) {
        Coupon coupon = findUsable(memberId, code);
        Money discount = coupon.calculateDiscount(itemsAmount, Instant.now());
        coupon.increaseUsage();
        redemptionRepository.save(new CouponRedemption(coupon.getId(), memberId, orderNumber, discount.getAmount()));
        return discount;
    }
    @Transactional
    public void release(String orderNumber) {
        redemptionRepository.findByOrderNumber(orderNumber).ifPresent(redemption -> {
            couponRepository.findById(redemption.getCouponId()).ifPresent(Coupon::decreaseUsage);
            redemptionRepository.delete(redemption);
        });
    }

    // ---------- 관리자 ----------

    public PageResponse<AdminCouponResponse> getCoupons(Pageable pageable) {
        return PageResponse.from(couponRepository.findAll(pageable).map(AdminCouponResponse::from));
    }

    public AdminCouponResponse getCoupon(Long couponId) {
        return AdminCouponResponse.from(find(couponId));
    }

    @Transactional
    public AdminCouponResponse create(CouponSaveRequest request) {
        if (couponRepository.existsByCode(Coupon.normalize(request.code()))) {
            throw new BusinessException(ErrorCode.COUPON_CODE_DUPLICATED);
        }
        Coupon coupon = new Coupon(request.code());
        apply(coupon, request);
        return AdminCouponResponse.from(couponRepository.save(coupon));
    }

    @Transactional
    public AdminCouponResponse update(Long couponId, CouponSaveRequest request) {
        Coupon coupon = find(couponId);
        if (!coupon.getCode().equals(Coupon.normalize(request.code()))) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        apply(coupon, request);
        return AdminCouponResponse.from(coupon);
    }

    private void apply(Coupon coupon, CouponSaveRequest request) {
        coupon.change(request.name(), request.discountType(), request.discountValue(), request.maxDiscountAmount(),
                request.minOrderAmount(), request.currency(), request.startAt(), request.endAt(),
                request.usageLimit(), request.perMemberLimit(), request.active());
    }

    private Coupon find(Long couponId) {
        return couponRepository.findById(couponId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));
    }
}