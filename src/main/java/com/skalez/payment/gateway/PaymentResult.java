package com.skalez.payment.gateway;

import com.skalez.global.money.Money;

public record PaymentResult(String paymentId, PaymentStatus status, Money paidAmount) {
}
