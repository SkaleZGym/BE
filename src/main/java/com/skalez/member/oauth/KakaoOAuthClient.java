package com.skalez.member.oauth;

import com.skalez.member.domain.SocialProvider;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * 카카오 로그인 (REST API).
 * 카카오 developers > 내 애플리케이션: REST API 키 = clientId, 보안 > Client Secret = clientSecret,
 * 카카오 로그인 > Redirect URI 에 {사이트}/api/auth/oauth/kakao/callback 등록,
 * 동의 항목에 카카오계정(이메일)·닉네임 (이메일은 비즈 앱 전환 후 '필수 동의'로 받을 수 있다)
 */
class KakaoOAuthClient implements OAuthClient {

    private static final String AUTHORIZE = "https://kauth.kakao.com/oauth/authorize";
    private static final ParameterizedTypeReference<Map<String, Object>> MAP = new ParameterizedTypeReference<>() {};
    private final RestClient restClient = RestClient.create();
    private final OAuthProperties.Client keys;

    KakaoOAuthClient(OAuthProperties.Client keys) {
        this.keys = keys;
    }

    @Override
    public SocialProvider provider() {
        return SocialProvider.KAKAO;
    }

    @Override
    public String authorizeUrl(String redirectUri, String state) {
        return UriComponentsBuilder.fromUriString(AUTHORIZE)
                .queryParam("response_type", "code")
                .queryParam("client_id", keys.clientId())
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", state)
                .encode()
                .toUriString();
    }

    @Override
    public Profile fetchProfile(String code, String redirectUri, String state) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", keys.clientId());
        form.add("client_secret", keys.clientSecret());
        form.add("redirect_uri", redirectUri);
        form.add("code", code);
        Map<String, Object> token = restClient.post().uri("https://kauth.kakao.com/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(MAP);
        String accessToken = token == null ? null : Json.text(token, "access_token");
        if (accessToken == null) throw new IllegalStateException("카카오 토큰 응답에 access_token 이 없습니다");

        Map<String, Object> me = restClient.get().uri("https://kapi.kakao.com/v2/user/me")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(MAP);
        if (me == null) throw new IllegalStateException("카카오 회원 정보 응답이 비어 있습니다");
        Map<String, Object> account = Json.object(me, "kakao_account");
        String email = Json.text(account, "email");
        boolean verified = Json.bool(account, "is_email_verified") && Json.bool(account, "is_email_valid");
        String name = Json.text(account, "name");
        if (name == null) name = Json.text(Json.object(account, "profile"), "nickname");
        return new Profile(Json.text(me, "id"), email, verified, name);
    }
}
