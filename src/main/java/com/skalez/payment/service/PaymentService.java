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

    public PaymentResult confirm(String paymentId, Money expectedAmount) {PaymentResult result = paymentGateway.fetchPayment(paymentId);
        if (result.status() != PaymentStatus.PAID) {throw new BusinessException(ErrorCode.PAYMENT_NOT_PAID);}
        if (!result.paidAmount().equals(expectedAmount)) {paymentGateway.cancel(paymentId, result.paidAmount(), "결제 금액 불일치");throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);}
        return result;
    }
    public void refund(String paymentId, Money amount, String reason) {paymentGateway.cancel(paymentId, amount, reason);}
}