package com.skalez.payment.gateway.portone;

import com.skalez.global.money.Money;
import com.skalez.payment.gateway.PaymentGateway;
import com.skalez.payment.gateway.PaymentResult;
import org.springframework.stereotype.Component;

@Component
public class PortOnePaymentGateway implements PaymentGateway {
    @Override
    public PaymentResult fetchPayment(String paymentId){throw new UnsupportedOperationException("포트원 연동 전입니다.");}
    @Override
    public void cancel(String paymentId, Money amount, String reason) {throw new UnsupportedOperationException("포트원 연동 전입니다.");}
}
