package com.skalez.coupon.repository;

import com.skalez.coupon.domain.CouponRedemption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, Long> {
    long countByCouponIdAndMemberId(Long couponId, Long memberId);
    Optional<CouponRedemption> findByOrderNumber(String orderNumber);
}
