package com.skalez.shipping.domain;

import com.skalez.global.error.BusinessException;
import com.skalez.global.error.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 통화별 배송비 설정 (관리자에서 수정). 금액은 최소 단위 (KRW=원, USD=센트).
 * 무료배송 기준은 할인 후 상품 금액으로 판단한다.
 */
@Entity
@Table(name = "shipping_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShippingSetting {

    /** KRW, USD 등. 통화당 한 줄 */
    @Id
    @Column(length = 3)
    private String currency;

    @Column(nullable = false)
    private long baseFee;

    /** 이 금액 이상이면 기본 배송비 무료. null이면 무료배송 없음 */
    private Long freeThreshold;

    /** 제주 추가 배송비 (무료배송이어도 붙는다) */
    @Column(nullable = false)
    private long jejuExtraFee;

    /** 도서산간 추가 배송비 */
    @Column(nullable = false)
    private long islandExtraFee;

    /** 배송 중으로 바꾼 뒤 이 일수가 지나면 자동으로 배송 완료 처리 */
    @Column(nullable = false)
    private int autoDeliverDays;

    @Column(nullable = false)
    private Instant updatedAt;

    public ShippingSetting(String currency, long baseFee, Long freeThreshold, long jejuExtraFee, long islandExtraFee, int autoDeliverDays) {
        this.currency = currency;
        change(baseFee, freeThreshold, jejuExtraFee, islandExtraFee, autoDeliverDays);
    }

    public void change(long baseFee, Long freeThreshold, long jejuExtraFee, long islandExtraFee, int autoDeliverDays) {
        if (baseFee < 0 || jejuExtraFee < 0 || islandExtraFee < 0 || autoDeliverDays < 1
                || (freeThreshold != null && freeThreshold < 0)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        this.baseFee = baseFee;
        this.freeThreshold = freeThreshold;
        this.jejuExtraFee = jejuExtraFee;
        this.islandExtraFee = islandExtraFee;
        this.autoDeliverDays = autoDeliverDays;
        this.updatedAt = Instant.now();
    }

    /** 기본 배송비 (지역 추가비 제외) */
    public long baseFeeFor(long itemsAmount) {
        if (itemsAmount == 0) {
            return 0;
        }
        return freeThreshold != null && itemsAmount >= freeThreshold ? 0 : baseFee;
    }

    public long extraFeeFor(RemoteAreaType type) {
        return switch (type) {
            case JEJU -> jejuExtraFee;
            case ISLAND -> islandExtraFee;
        };
    }
}