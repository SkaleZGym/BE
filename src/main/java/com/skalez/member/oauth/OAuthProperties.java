package com.skalez.member.oauth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 간편 로그인 키 (카카오 developers · 네이버 developers 에서 발급). 환경변수로만 넣는다.
 * 키가 비어 있으면 그 로그인은 꺼진다.
 * fake=true(로컬 전용)면 키가 없을 때 진짜 카카오·네이버 대신 시험용 계정으로 흐름만 확인할 수 있다.
 */
@ConfigurationProperties("skalez.oauth")
public record OAuthProperties(boolean fake, Client kakao, Client naver) {

    public record Client(String clientId, String clientSecret) {
        public boolean configured() {
            return clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank();
        }
    }
}
