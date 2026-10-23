package com.skalez.payment.gateway.nice;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** 나이스페이먼츠 API 요청·응답 (필요한 필드만) */
public final class NicePayDtos {

    private NicePayDtos() {
    }

    /**
     * 승인 POST /v1/payments/{tid}
     * signData = hex(sha256(tid + amount + ediDate + 시크릿키))
     */
    public record ApproveRequest(long amount, String ediDate, String signData) {
    }

    /**
     * 취소 POST /v1/payments/{tid}/cancel  (cancelAmt를 안 보내면 전액 취소)
     * signData = hex(sha256(tid + ediDate + 시크릿키))
     */
    public record CancelRequest(String reason, String orderId, String ediDate, String signData) {
    }

    /**
     * 결제 응답. resultCode "0000" = 성공
     * status: paid, ready, failed, cancelled, partialCancelled, expired
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PaymentResponse(
            String resultCode,
            String resultMsg,
            String tid,
            String orderId,
            String status,
            long amount,
            String currency
    ) {
        public boolean isSuccess() {
            return "0000".equals(resultCode);
        }
    }
}
