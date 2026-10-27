package com.skalez.claim.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 신청에 들어간 주문 상품 한 줄. exchangeSize는 교환일 때 바꿀 사이즈 (같은 사이즈로 교환이면 비움) */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClaimItem {

    @Column(nullable = false)
    private Long orderItemId;

    @Column(nullable = false)
    private int quantity;

    @Column(length = 10)
    private String exchangeSize;

    public ClaimItem(Long orderItemId, int quantity, String exchangeSize) {
        this.orderItemId = orderItemId;
        this.quantity = quantity;
        this.exchangeSize = exchangeSize;
    }
}
