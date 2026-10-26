package com.skalez.coupon.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "coupon_redemptions", indexes = @Index(columnList = "coupon_id, member_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponRedemption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "coupon_id", nullable = false)
    private Long couponId;
    @Column(name = "member_id", nullable = false)
    private Long memberId;
    @Column(nullable = false, unique = true, length = 30)
    private String orderNumber;
    @Column(nullable =false)
    private long discountAmount;
    @Column(nullable = false, updatable = false)
    private Instant redeemedAt;
    public CouponRedemption(Long couponId, Long memberId, String orderNumber, long discountAmount){
        this.couponId = couponId;
        this.memberId = memberId;
        this.orderNumber = orderNumber;
        this.discountAmount = discountAmount;
        this.redeemedAt = Instant.now();
    }
}
