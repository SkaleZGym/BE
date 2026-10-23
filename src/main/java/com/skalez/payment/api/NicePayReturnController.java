package com.skalez.payment.api;

import com.skalez.global.error.BusinessException;
import com.skalez.order.service.OrderService;
import com.skalez.payment.gateway.nice.NicePayGateway;
import com.skalez.payment.gateway.nice.NicePayProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Optional;

/**
 * 나이스페이 결제창 returnUrl.
 * 고객 브라우저가 결제 인증을 마치면 나이스페이가 이 주소로 결과를 form POST 한다 (로그인 토큰 없음).
 * 서명 확인 → 승인 → 고객 브라우저를 FE 결과 페이지로 돌려보낸다.
 * FE 결과 페이지: {resultRedirectUrl}?orderNumber=...&status=success|fail&code=...
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(name = "skalez.payment.provider", havingValue = "nice")
public class NicePayReturnController {

    private final NicePayGateway nicePayGateway;
    private final NicePayProperties properties;
    private final OrderService orderService;

    @PostMapping(value = "/api/payments/nice/return", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> handleReturn(
            @RequestParam String authResultCode,
            @RequestParam(required = false) String authResultMsg,
            @RequestParam(required = false) String tid,
            @RequestParam(required = false) String clientId,
            @RequestParam String orderId,
            @RequestParam(required = false) String amount,
            @RequestParam(required = false) String authToken,
            @RequestParam(required = false) String signature) {

        // 1) 고객이 결제창을 닫았거나 카드 인증이 실패한 경우 → 주문은 결제 대기로 남고 다시 결제할 수 있다
        if (!"0000".equals(authResultCode)) {
            log.info("나이스페이 인증 실패: orderId={}, code={}, msg={}", orderId, authResultCode, authResultMsg);
            return redirect(orderId, "fail", authResultCode);
        }
        // 2) 위조 방지: 나이스페이 서명 확인
        if (!nicePayGateway.isValidAuthResult(authToken, clientId, amount, signature)) {
            log.warn("나이스페이 서명 불일치: orderId={}", orderId);
            return redirect(orderId, "fail", "INVALID_SIGNATURE");
        }
        // 3) 승인 (금액은 서버 주문 금액 기준으로 다시 검증)
        try {
            orderService.confirmPaymentByPg(orderId, tid, Long.parseLong(amount));
            return redirect(orderId, "success", null);
        } catch (BusinessException e) {
            log.warn("나이스페이 승인 처리 실패: orderId={}, code={}", orderId, e.getErrorCode());
            return redirect(orderId, "fail", e.getErrorCode().name());
        } catch (NumberFormatException e) {
            return redirect(orderId, "fail", "INVALID_AMOUNT");
        }
    }

    private ResponseEntity<Void> redirect(String orderNumber, String status, String code) {
        URI location = UriComponentsBuilder.fromUriString(properties.resultRedirectUrl())
                .queryParam("orderNumber", orderNumber)
                .queryParam("status", status)
                .queryParamIfPresent("code", Optional.ofNullable(code))
                .encode()
                .build()
                .toUri();
        return ResponseEntity.status(HttpStatus.SEE_OTHER).header(HttpHeaders.LOCATION, location.toString()).build();
    }
}
