package com.skalez.global.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("skalez.admin")
public record AdminProperties(String email, String password, String name) {
}
