package com.skalez.global.security.crypto;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("skalez.crypto")
public record CryptoProperties(String aesKey) {
}