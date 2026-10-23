package com.skalez.payment.gateway;

import com.skalez.global.money.Money;

/**
 * PG(결제대행사)를 감싸는 인터페이스. 서비스 코드는 어느 PG인지 모른다.
 * 지금은 나이스페이먼츠, 나중에 KG이니시스·해외 PG(PayPal 등)를 같은 인터페이스로 추가할 수 있다.
 */
public interface PaymentGateway {

    /**
     * 결제 승인. 고객이 결제창에서 인증을 마친 뒤 서버가 호출해야 실제로 결제가 확정된다.
     *
     * @param paymentKey  PG가 발급한 결제 키 (나이스페이는 tid)
     * @param orderNumber 우리 주문번호 (결제창 열 때 PG에 보낸 orderId)
     * @param amount      서버가 계산한 주문 금액. 클라이언트가 보낸 금액은 절대 믿지 않는다
     */
    PaymentResult confirm(String paymentKey, String orderNumber, Money amount);

    /** 결제 전액 취소(환불) */
    void cancel(String paymentKey, String orderNumber, Money amount, String reason);
}
