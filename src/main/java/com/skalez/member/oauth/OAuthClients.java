package com.skalez.member.oauth;

import com.skalez.global.error.BusinessException;
import com.skalez.global.error.ErrorCode;
import com.skalez.member.domain.SocialProvider;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/** 설정에 따라 쓸 수 있는 간편 로그인을 고른다: 키가 있으면 진짜, 없고 fake=true 면 시험용, 둘 다 아니면 꺼짐 */
@Component
public class OAuthClients {

    private final Map<SocialProvider, OAuthClient> clients = new EnumMap<>(SocialProvider.class);

    public OAuthClients(OAuthProperties properties) {
        register(SocialProvider.KAKAO, properties.kakao(), properties.fake(), KakaoOAuthClient::new);
        register(SocialProvider.NAVER, properties.naver(), properties.fake(), NaverOAuthClient::new);
    }

    private void register(SocialProvider provider, OAuthProperties.Client keys, boolean fake,
                          java.util.function.Function<OAuthProperties.Client, OAuthClient> real) {
        if (keys != null && keys.configured()) {
            clients.put(provider, real.apply(keys));
        } else if (fake) {
            clients.put(provider, new FakeOAuthClient(provider));
        }
    }

    public OAuthClient get(SocialProvider provider) {
        return Optional.ofNullable(clients.get(provider)).orElseThrow(() -> new BusinessException(ErrorCode.SOCIAL_LOGIN_NOT_CONFIGURED));
    }

    public boolean enabled(SocialProvider provider) {
        return clients.containsKey(provider);
    }
}
