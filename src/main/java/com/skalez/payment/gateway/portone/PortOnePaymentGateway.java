package com.skalez.payment.gateway.portone;

import com.skalez.global.money.Money;
import com.skalez.payment.gateway.PaymentGateway;
import com.skalez.payment.gateway.PaymentResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 해외 결제 등 PG를 여러 개 붙일 때 쓸 자리. skalez.payment.provider=portone 일 때만 등록된다 */
@Component
@ConditionalOnProperty(name = "skalez.payment.provider", havingValue = "portone")
public class PortOnePaymentGateway implements PaymentGateway {

    @Override
    public PaymentResult confirm(String paymentKey, String orderNumber, Money amount) {
        throw new UnsupportedOperationException("포트원 연동 전입니다.");
    }

    @Override
    public void cancel(String paymentKey, String orderNumber, Money amount, String reason) {
        throw new UnsupportedOperationException("포트원 연동 전입니다.");
    }
}
