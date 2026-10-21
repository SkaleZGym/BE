package com.skalez.product.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PriceTag {

    @Column(name = "amount", nullable = false)
    private long amount;
    @Column(name = "list_amount")
    private Long listAmount;
    public PriceTag(long amount, Long listAmount) {
        this.amount = amount;
        this.listAmount = listAmount;
    }
}