package com.skalez.order.domain;

import com.skalez.global.money.Money;
public final class ShippingPolicy {

    private ShippingPolicy() {
    }

    public static Money feeFor(Money itemsAmount) {
        String currency = itemsAmount.getCurrency();
        if (itemsAmount.getAmount() == 0) {
            return Money.zero(currency);
        }
        return switch (currency) {
            case "KRW" -> itemsAmount.isGreaterThanOrEqual(Money.of(100_000, "KRW"))
                    ? Money.zero("KRW")
                    : Money.of(3_000, "KRW");
            case "USD" -> itemsAmount.isGreaterThanOrEqual(Money.of(150_00, "USD"))
                    ? Money.zero("USD")
                    : Money.of(15_00, "USD");
            default -> throw new IllegalArgumentException("배송비 정책이 없는 통화입니다: " + currency);
        };
    }
}