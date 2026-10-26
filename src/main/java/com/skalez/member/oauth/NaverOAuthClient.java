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
 * 네이버 로그인.
 * 네이버 developers > 애플리케이션: Client ID / Client Secret,
 * 서비스 URL 과 Callback URL({사이트}/api/auth/oauth/naver/callback) 등록, 제공 정보에 이메일·이름 체크.
 * 네이버 이메일은 네이버가 확인한 주소라 같은 이메일의 기존 회원과 이을 수 있다
 */
class NaverOAuthClient implements OAuthClient {

    private static final String AUTHORIZE = "https://nid.naver.com/oauth2.0/authorize";
    private static final ParameterizedTypeReference<Map<String, Object>> MAP = new ParameterizedTypeReference<>() {};
    private final RestClient restClient = RestClient.create();
    private final OAuthProperties.Client keys;

    NaverOAuthClient(OAuthProperties.Client keys) {
        this.keys = keys;
    }

    @Override
    public SocialProvider provider() {
        return SocialProvider.NAVER;
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
        form.add("code", code);
        form.add("state", state);
        Map<String, Object> token = restClient.post().uri("https://nid.naver.com/oauth2.0/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(MAP);
        String accessToken = token == null ? null : Json.text(token, "access_token");
        if (accessToken == null) throw new IllegalStateException("네이버 토큰 응답에 access_token 이 없습니다");

        Map<String, Object> me = restClient.get().uri("https://openapi.naver.com/v1/nid/me")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(MAP);
        Map<String, Object> response = me == null ? Map.of() : Json.object(me, "response");
        if (Json.text(response, "id") == null) throw new IllegalStateException("네이버 회원 정보 응답이 비어 있습니다");
        String email = Json.text(response, "email");
        String name = Json.text(response, "name");
        if (name == null) name = Json.text(response, "nickname");
        return new Profile(Json.text(response, "id"), email, email != null, name);
    }
}
