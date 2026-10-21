package com.skalez.global.money;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Currency;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Money {

    @Column(name = "amount", nullable = false)
    private long amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency; // ISO 4217: KRW, USD, JPY, CNY

    private Money(long amount, String currency) {
        this.amount = amount;
        this.currency = currency;
    }

    public static Money of(long amount, String currencyCode) {if (amount < 0) {throw new IllegalArgumentException("금액은 음수일 수 없습니다: " + amount);
        }
        Currency.getInstance(currencyCode);
        return new Money(amount, currencyCode);
    }

    public static Money zero(String currencyCode) {return of(0, currencyCode);
    }

    public Money plus(Money other) {requireSameCurrency(other);
        return new Money(Math.addExact(amount, other.amount), currency);
    }

    public Money times(int quantity) {
        return new Money(Math.multiplyExact(amount, quantity), currency);
    }

    public boolean isGreaterThanOrEqual(Money other) {requireSameCurrency(other);
        return amount >= other.amount;
    }
    private void requireSameCurrency(Money other) {if (!currency.equals(other.currency)) {throw new IllegalArgumentException("통화가 다릅니다: " + currency + " / " + other.currency);
        }
    }
}