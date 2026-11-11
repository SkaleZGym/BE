package com.skalez.payment.gateway;

import com.skalez.global.error.BusinessException;
import com.skalez.global.error.ErrorCode;
import com.skalez.global.money.Money;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * PG 계약 전 운영용: 서버는 뜨고 상품·회원·관리자 화면은 다 쓰되, 결제만 '준비 중'으로 막는다.
 * 가짜 결제(fake)와 달리 결제 완료를 만들 수 없어서 운영에서 써도 안전하다.
 * skalez.payment.provider=none 일 때만 등록된다 (환경변수 PAYMENT_PROVIDER=none)
 */
@Component
@ConditionalOnProperty(name = "skalez.payment.provider", havingValue = "none")
public class DisabledPaymentGateway implements PaymentGateway {

    @Override
    public PaymentResult confirm(String paymentKey, String orderNumber, Money amount) {
        throw new BusinessException(ErrorCode.PAYMENT_NOT_READY);
    }

    @Override
    public void cancel(String paymentKey, String orderNumber, Money amount, String reason) {
        throw new BusinessException(ErrorCode.PAYMENT_NOT_READY);
    }
}
