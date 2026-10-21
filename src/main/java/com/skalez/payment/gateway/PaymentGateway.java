package com.skalez.payment.gateway;

import com.skalez.global.money.Money;

public interface PaymentGateway {
    PaymentResult fetchPayment(String paymentId);
    void cancel(String paymentId, Money amount, String reason);
}
