package com.skalez.member.oauth;

import com.skalez.global.error.BusinessException;
import com.skalez.member.domain.Member;
import com.skalez.member.domain.SocialProvider;
import com.skalez.member.repository.MemberRepository;
import com.skalez.member.repository.SocialAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 로컬 시험용 간편 로그인(skalez.oauth.fake=true)으로 흐름 전체를 확인한다 */
@SpringBootTest
@Transactional
class SocialAuthServiceTest {

    private static final String KAKAO_CALLBACK = "http://localhost:3010/api/auth/oauth/kakao/callback";

    @Autowired SocialAuthService socialAuthService;
    @Autowired MemberRepository memberRepository;
    @Autowired SocialAccountRepository socialAccountRepository;

    @Test
    void firstVisitNeedsConsentThenLogsInDirectly() {
        SocialAuthService.Result first = socialAuthService.login(SocialProvider.KAKAO, "code", KAKAO_CALLBACK, "state");
        assertThat(first.token()).isNull();
        assertThat(first.ticket()).isNotBlank();
        assertThat(first.email()).isEqualTo("kakao.tester@skalez.test");

        var token = socialAuthService.completeSignup(first.ticket(), "카카오회원", true, Locale.KOREAN);
        assertThat(token.accessToken()).isNotBlank();
        Member member = memberRepository.findByEmail("kakao.tester@skalez.test").orElseThrow();
        assertThat(member.getTermsAgreedAt()).isNotNull();
        assertThat(member.isMarketingConsent()).isTrue();

        // 같은 가입 표는 다시 못 쓴다
        assertThatThrownBy(() -> socialAuthService.completeSignup(first.ticket(), null, false, Locale.KOREAN)).isInstanceOf(BusinessException.class);

        // 두 번째부터는 바로 로그인
        SocialAuthService.Result again = socialAuthService.login(SocialProvider.KAKAO, "code", KAKAO_CALLBACK, "state");
        assertThat(again.token()).isNotNull();
        assertThat(again.ticket()).isNull();
    }

    @Test
    void verifiedEmailLinksToExistingMember() {
        Member legacy = memberRepository.save(Member.importLegacy(Member.LEGACY_IMWEB, null, "naver.tester@skalez.test", "아임웹회원", null, null, false, null));

        SocialAuthService.Result result = socialAuthService.login(SocialProvider.NAVER, "code", "http://localhost:3010/api/auth/oauth/naver/callback", "state");

        assertThat(result.token()).isNotNull();
        assertThat(socialAccountRepository.existsByMemberIdAndProvider(legacy.getId(), SocialProvider.NAVER)).isTrue();
    }

    @Test
    void rejectsForeignRedirectUri() {
        assertThatThrownBy(() -> socialAuthService.authorizeUrl(SocialProvider.KAKAO, "https://evil.example/callback", "state"))
                .isInstanceOf(BusinessException.class);
    }
}
