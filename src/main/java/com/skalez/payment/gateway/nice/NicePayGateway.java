package com.skalez.payment.gateway.nice;

import com.skalez.global.error.BusinessException;
import com.skalez.global.error.ErrorCode;
import com.skalez.global.money.Money;
import com.skalez.payment.gateway.PaymentGateway;
import com.skalez.payment.gateway.PaymentResult;
import com.skalez.payment.gateway.PaymentStatus;
import com.skalez.payment.gateway.nice.NicePayDtos.ApproveRequest;
import com.skalez.payment.gateway.nice.NicePayDtos.CancelRequest;
import com.skalez.payment.gateway.nice.NicePayDtos.PaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

/**
 * 나이스페이먼츠 결제 연동 (Server 승인 모델 · 국내 카드·간편결제·계좌이체).
 *
 * 흐름: FE가 AUTHNICE.requestPay()로 결제창을 연다 (orderId = 우리 주문번호, returnUrl = 우리 서버)
 *      → 고객 인증 성공 → 나이스페이가 returnUrl(NicePayReturnController)로 tid·authToken·signature를 POST
 *      → 서버가 서명 확인 후 여기서 승인 API 호출 → 승인돼야 실제 결제 완료
 *
 * 인증: Authorization: Basic base64("{클라이언트키}:{시크릿키}")
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "skalez.payment.provider", havingValue = "nice")
public class NicePayGateway implements PaymentGateway {

    private static final String SUPPORTED_CURRENCY = "KRW";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final NicePayProperties properties;
    private final RestClient restClient;

    public NicePayGateway(NicePayProperties properties) {
        if (isBlank(properties.clientKey()) || isBlank(properties.secretKey())) {
            throw new IllegalStateException("NICEPAY_CLIENT_KEY / NICEPAY_SECRET_KEY 환경변수가 없습니다. 나이스페이 가맹점 관리자 > 개발정보에서 발급받아 넣어 주세요.");
        }
        this.properties = properties;
        String basic = Base64.getEncoder().encodeToString(
                (properties.clientKey() + ":" + properties.secretKey()).getBytes(StandardCharsets.UTF_8));

        // 승인은 카드사까지 다녀오므로 넉넉히 기다린다
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(30));

        this.restClient = RestClient.builder()
                .baseUrl(properties.apiUrl())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + basic)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * returnUrl로 들어온 인증 결과가 진짜 나이스페이가 보낸 것인지 확인.
     * signature = hex(sha256(authToken + clientId + amount + 시크릿키))
     */
    public boolean isValidAuthResult(String authToken, String clientId, String amount, String signature) {
        if (isBlank(authToken) || isBlank(signature) || !properties.clientKey().equals(clientId)) {
            return false;
        }
        String expected = sha256Hex(authToken + clientId + amount + properties.secretKey());
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public PaymentResult confirm(String tid, String orderNumber, Money amount) {
        requireKrw(amount);
        String ediDate = now();
        String signData = sha256Hex(tid + amount.getAmount() + ediDate + properties.secretKey());
        PaymentResponse response;
        try {
            response = restClient.post()
                    .uri("/v1/payments/{tid}", tid)
                    .body(new ApproveRequest(amount.getAmount(), ediDate, signData))
                    .retrieve()
                    .body(PaymentResponse.class);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
                log.error("나이스페이 인증 실패: 클라이언트 키·시크릿 키를 확인하세요. orderNumber={}", orderNumber);
                throw new BusinessException(ErrorCode.PAYMENT_GATEWAY_ERROR);
            }
            log.warn("나이스페이 승인 실패: orderNumber={}, status={}, body={}", orderNumber, e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(e.getStatusCode().is4xxClientError() ? ErrorCode.PAYMENT_NOT_PAID : ErrorCode.PAYMENT_GATEWAY_ERROR);
        } catch (ResourceAccessException e) {
            // 응답을 못 받으면 승인됐는지 알 수 없다 → 나이스페이 권고대로 취소를 걸어 돈이 빠져나간 채 남지 않게 한다
            log.error("나이스페이 승인 응답 없음(타임아웃): orderNumber={} → 취소 시도", orderNumber, e);
            try {
                cancel(tid, orderNumber, amount, "승인 응답 시간 초과");
            } catch (BusinessException cancelFailed) {
                log.error("타임아웃 후 취소도 실패. 관리자 확인 필요: orderNumber={}, tid={}", orderNumber, tid);
            }
            throw new BusinessException(ErrorCode.PAYMENT_GATEWAY_ERROR);
        }

        if (response == null || !response.isSuccess()) {
            log.warn("나이스페이 승인 거절: orderNumber={}, code={}, msg={}", orderNumber,
                    response == null ? null : response.resultCode(), response == null ? null : response.resultMsg());
            return new PaymentResult(tid, PaymentStatus.FAILED, null);
        }
        if (!orderNumber.equals(response.orderId())) {
            log.error("나이스페이 승인 응답의 주문번호가 다름: 요청={}, 응답={}", orderNumber, response.orderId());
            throw new BusinessException(ErrorCode.PAYMENT_GATEWAY_ERROR);
        }
        String currency = isBlank(response.currency()) ? SUPPORTED_CURRENCY : response.currency();
        return new PaymentResult(response.tid(), toStatus(response.status()), Money.of(response.amount(), currency));
    }

    @Override
    public void cancel(String tid, String orderNumber, Money amount, String reason) {
        String ediDate = now();
        String signData = sha256Hex(tid + ediDate + properties.secretKey());
        try {
            PaymentResponse response = restClient.post()
                    .uri("/v1/payments/{tid}/cancel", tid)
                    .body(new CancelRequest(reason, orderNumber, ediDate, signData))
                    .retrieve()
                    .body(PaymentResponse.class);
            if (response == null || !response.isSuccess()) {
                log.error("나이스페이 취소 거절: orderNumber={}, code={}, msg={}", orderNumber,
                        response == null ? null : response.resultCode(), response == null ? null : response.resultMsg());
                throw new BusinessException(ErrorCode.PAYMENT_GATEWAY_ERROR);
            }
        } catch (RestClientResponseException | ResourceAccessException e) {
            // 환불 실패는 돈 문제라 조용히 넘기면 안 된다 → 주문 취소도 함께 롤백된다
            log.error("나이스페이 취소 실패: orderNumber={}, tid={}", orderNumber, tid, e);
            throw new BusinessException(ErrorCode.PAYMENT_GATEWAY_ERROR);
        }
    }

    private static PaymentStatus toStatus(String niceStatus) {
        if (niceStatus == null) {
            return PaymentStatus.FAILED;
        }
        return switch (niceStatus) {
            case "paid" -> PaymentStatus.PAID;
            case "ready" -> PaymentStatus.READY;
            case "cancelled", "partialCancelled" -> PaymentStatus.CANCELLED;
            default -> PaymentStatus.FAILED; // failed, expired
        };
    }

    /** 나이스페이 국내 결제는 원화만. 해외(USD 등)는 2027에 다른 PG로 붙인다 */
    private static void requireKrw(Money amount) {
        if (!SUPPORTED_CURRENCY.equals(amount.getCurrency())) {
            throw new BusinessException(ErrorCode.PAYMENT_CURRENCY_NOT_SUPPORTED);
        }
    }

    /** 전문 생성 시각 (ISO 8601, 한국 시간) */
    private static String now() {
        return OffsetDateTime.now(KST).truncatedTo(ChronoUnit.SECONDS).toString();
    }

    private static String sha256Hex(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
