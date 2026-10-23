package com.skalez.payment.service;

import com.skalez.global.error.BusinessException;
import com.skalez.global.error.ErrorCode;
import com.skalez.global.money.Money;
import com.skalez.payment.gateway.PaymentGateway;
import com.skalez.payment.gateway.PaymentResult;
import com.skalez.payment.gateway.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentGateway paymentGateway;

    /** PG에 승인 요청 → 결제 완료 + 금액 일치까지 확인. 금액이 다르면 바로 환불한다 */
    public PaymentResult confirm(String paymentKey, String orderNumber, Money expectedAmount) {
        PaymentResult result = paymentGateway.confirm(paymentKey, orderNumber, expectedAmount);
        if (result.status() != PaymentStatus.PAID) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_PAID);
        }
        if (!result.paidAmount().equals(expectedAmount)) {
            paymentGateway.cancel(paymentKey, orderNumber, result.paidAmount(), "결제 금액 불일치");
            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }
        return result;
    }

    public void refund(String paymentKey, String orderNumber, Money amount, String reason) {
        paymentGateway.cancel(paymentKey, orderNumber, amount, reason);
    }
}
