package com.skalez.member.oauth;

import com.skalez.member.domain.SocialProvider;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Locale;

/**
 * 로컬 전용 시험용 간편 로그인 (skalez.oauth.fake=true 이고 키가 없을 때만).
 * 진짜 카카오·네이버 창 대신 바로 FE 콜백으로 돌아가고, 고정된 시험 계정 정보를 돌려준다.
 * 운영(prod)에서는 켜지 않는다
 */
class FakeOAuthClient implements OAuthClient {

    private final SocialProvider provider;

    FakeOAuthClient(SocialProvider provider) {
        this.provider = provider;
    }

    @Override
    public SocialProvider provider() {
        return provider;
    }

    @Override
    public String authorizeUrl(String redirectUri, String state) {
        return UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("code", "fake-code")
                .queryParam("state", state)
                .encode()
                .toUriString();
    }

    @Override
    public Profile fetchProfile(String code, String redirectUri, String state) {
        String id = provider.name().toLowerCase(Locale.ROOT);
        return new Profile(id + "-test-user", id + ".tester@skalez.test", true, provider == SocialProvider.KAKAO ? "카카오시험" : "네이버시험");
    }
}
