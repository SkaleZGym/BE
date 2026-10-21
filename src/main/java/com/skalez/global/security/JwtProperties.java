package com.skalez.global.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;

@ConfigurationProperties("skalez.jwt")
public record JwtProperties(String secret, Duration accessTokenTtl) {
}