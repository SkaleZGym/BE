package com.skalez.payment.gateway.nice;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 나이스페이먼츠 설정 (가맹점 관리자 > 개발정보에서 발급).
 * clientKey: 결제창 호출용. FE에 노출돼도 되는 값
 * secretKey: 서버 API 인증·서명용. 서버에서만 쓰고 FE·git에 절대 넣지 않는다
 * apiUrl: 운영 https://api.nicepay.co.kr / 샌드박스 https://sandbox-api.nicepay.co.kr
 * resultRedirectUrl: 결제 처리 후 고객 브라우저를 돌려보낼 FE 주소 (예: https://skalez.com/order/result)
 */
@ConfigurationProperties("skalez.payment.nice")
public record NicePayProperties(String clientKey, String secretKey, String apiUrl, String resultRedirectUrl) {
}
