package com.skalez.global.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;

@ConfigurationProperties("skalez.jwt")
/** refreshTokenTtl: 로그인 유지(리프레시 토큰) 기간. 비우면 30일 */
public record JwtProperties(String secret, Duration accessTokenTtl, Duration refreshTokenTtl) {
}