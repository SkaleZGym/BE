package com.skalez.coupon.domain;

import com.skalez.global.error.BusinessException;
import com.skalez.global.error.ErrorCode;
import com.skalez.global.money.Money;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Locale;

/**
 * 할인코드. 고객이 주문서에서 코드를 입력하면 상품 금액에서 할인한다 (배송비는 할인하지 않음).
 * 통화별로 만든다: KRW 쿠폰은 원화 주문에만 쓸 수 있다.
 */
@Entity
@Table(name = "coupons")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 고객이 입력하는 코드. 대문자로 저장한다. 예: WELCOME10 */
    @Column(nullable = false, unique = true, length = 30)
    private String code;

    /** 관리자용 이름. 예: 신규 가입 10% */
    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR) // enum을 varchar로 저장. H2·PostgreSQL 모두 같은 타입이라 Flyway 스키마 하나로 검증된다
    @Column(nullable = false, length = 10)
    private DiscountType discountType;

    @Column(nullable = false)
    private long discountValue;

    /** 정률 할인일 때 최대 할인 금액. null이면 제한 없음 */
    private Long maxDiscountAmount;

    /** 이 금액(상품 합계) 이상일 때만 사용 가능. 0이면 조건 없음 */
    @Column(nullable = false)
    private long minOrderAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    private Instant startAt;

    private Instant endAt;

    private Integer usageLimit;

    @Column(nullable = false)
    private int perMemberLimit;

    @Column(nullable = false)
    private int usedCount;

    @Column(nullable = false)
    private boolean active;

    @Version
    private long version;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public Coupon(String code) {
        this.code = normalize(code);
        this.createdAt = Instant.now();
    }
    public static String normalize(String code) {return code == null ? null : code.trim().toUpperCase(Locale.ROOT);}
    public void change(String name, DiscountType discountType, long discountValue, Long maxDiscountAmount, long minOrderAmount, String currency, Instant startAt, Instant endAt, Integer usageLimit, int perMemberLimit, boolean active) {
        if (discountType == DiscountType.PERCENT && (discountValue < 1 || discountValue > 100)) {throw new BusinessException(ErrorCode.INVALID_INPUT);}
        if (discountValue < 1 || minOrderAmount < 0 || perMemberLimit < 1 || (usageLimit != null && usageLimit < 1) || (maxDiscountAmount != null && maxDiscountAmount < 1)) {throw new BusinessException(ErrorCode.INVALID_INPUT);}
        if (startAt != null && endAt != null && !endAt.isAfter(startAt)) {throw new BusinessException(ErrorCode.INVALID_INPUT);}
        this.name = name;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.maxDiscountAmount = maxDiscountAmount;
        this.minOrderAmount = minOrderAmount;
        this.currency = currency;
        this.startAt = startAt;
        this.endAt = endAt;
        this.usageLimit = usageLimit;
        this.perMemberLimit = perMemberLimit;
        this.active = active;
    }
    public Money calculateDiscount(Money itemsAmount, Instant now) {
        if (!active || !currency.equals(itemsAmount.getCurrency()) || (startAt != null && now.isBefore(startAt)) || (endAt != null && !now.isBefore(endAt))) {throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE);}
        if (usageLimit != null && usedCount >= usageLimit) {throw new BusinessException(ErrorCode.COUPON_USAGE_EXCEEDED);}
        if (itemsAmount.getAmount() < minOrderAmount) {throw new BusinessException(ErrorCode.COUPON_MIN_ORDER_NOT_MET);}
        long discount = switch (discountType) {
            case PERCENT -> itemsAmount.getAmount() * discountValue / 100; // 원 단위 이하는 버림
            case FIXED -> discountValue;
        };
        if (maxDiscountAmount != null) {discount = Math.min(discount, maxDiscountAmount);}
        return Money.of(Math.min(discount, itemsAmount.getAmount()), currency);
    }
    public void increaseUsage() {this.usedCount++;}
    public void decreaseUsage() {if (usedCount > 0) {this.usedCount--;}}
}