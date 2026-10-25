package com.skalez.global.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 브라우저(FE)에서 API를 부를 수 있는 주소 목록 (skalez.cors.allowed-origins).
 * 운영은 환경변수 CORS_ALLOWED_ORIGINS에 쉼표로 구분해 넣는다.
 * 예: https://skalez.com,https://www.skalez.com,https://admin.skalez.com
 * 와일드카드 패턴도 된다: https://skalez-*.vercel.app
 */
@ConfigurationProperties("skalez.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
