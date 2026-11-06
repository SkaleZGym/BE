package com.skalez.global.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;

@ConfigurationProperties("skalez.jwt")
/**
 * refreshTokenTtl: 로그인 유지(리프레시 토큰) 기간. 비우면 30일.
 * adminAccessTokenTtl: 관리자 페이지 토큰 기간 (로그인 유지 없음 — 지나면 다시 비밀번호 + OTP). 비우면 1시간
 */
public record JwtProperties(String secret, Duration accessTokenTtl, Duration refreshTokenTtl, Duration adminAccessTokenTtl) {
}
