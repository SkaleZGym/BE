package com.skalez.global.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 고객이 보는 쇼핑몰(FE) 주소. 메일 속 링크를 만들 때 쓴다 (예: https://skalez.com) */
@ConfigurationProperties("skalez.site")
public record SiteProperties(String url) {
}
