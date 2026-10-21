package com.skalez.payment.gateway;

import com.skalez.global.money.Money;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 로컬 개발 전용 가짜 PG. PG 계약 전에도 주문 → 결제 흐름을 끝까지 테스트할 수 있다.
 * paymentId를 "test_{통화}_{금액}" 형식으로 보내면 그 금액이 결제된 것으로 응답한다.
 * 예: test_KRW_72000 → 72,000원 결제 완료
 */
@Component
@Primary
@Profile("local")
public class LocalFakePaymentGateway implements PaymentGateway {

    @Override
    public PaymentResult fetchPayment(String paymentId) {
        String[] parts = paymentId.split("_");
        if (parts.length != 3 || !parts[0].equals("test")) {return new PaymentResult(paymentId, PaymentStatus.FAILED, null);}
        Money paid = Money.of(Long.parseLong(parts[2]), parts[1]);
        return new PaymentResult(paymentId, PaymentStatus.PAID, paid);
    }

    @Override
    public void cancel(String paymentId, Money amount, String reason) {
        // 실제 PG가 없으니 아무것도 하지 않는다
    }
}