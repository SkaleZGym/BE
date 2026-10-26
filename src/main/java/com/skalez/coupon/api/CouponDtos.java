package com.skalez.coupon.api;

import com.skalez.coupon.domain.Coupon;
import com.skalez.coupon.domain.DiscountType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class CouponDtos {

    private CouponDtos(){}
    public record CouponPreviewRequest(@NotBlank @Size(max = 30) String code) {}
    public record CouponPreviewResponse(String code, String name, String currency, long itemsAmount, long discountAmount, long shippingFee, long totalAmount) { }

    /** 관리자 |쿠폰 등록·수정 */
    public record CouponSaveRequest(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{3,30}") String code,
            @NotBlank @Size(max = 100) String name,
            @NotNull DiscountType discountType,
            @Min(1) long discountValue,
            @Min(1) Long maxDiscountAmount,
            @Min(0) long minOrderAmount,
            @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
            Instant startAt,
            Instant endAt,
            @Min(1) Integer usageLimit,
            @Min(1) int perMemberLimit,
            boolean active
    ) { }
    public record AdminCouponResponse(
            Long id,
            String code,
            String name,
            String discountType,
            long discountValue,
            Long maxDiscountAmount,
            long minOrderAmount,
            String currency,
            Instant startAt,
            Instant endAt,
            Integer usageLimit,
            int perMemberLimit,
            int usedCount,
            boolean active,
            Instant createdAt
    ) {
        public static AdminCouponResponse from(Coupon coupon) {
            return new AdminCouponResponse(
                    coupon.getId(),
                    coupon.getCode(),
                    coupon.getName(),
                    coupon.getDiscountType().name(),
                    coupon.getDiscountValue(),
                    coupon.getMaxDiscountAmount(),
                    coupon.getMinOrderAmount(),
                    coupon.getCurrency(),
                    coupon.getStartAt(),
                    coupon.getEndAt(),
                    coupon.getUsageLimit(),
                    coupon.getPerMemberLimit(),
                    coupon.getUsedCount(),
                    coupon.isActive(),
                    coupon.getCreatedAt()
            );
        }
    }
}