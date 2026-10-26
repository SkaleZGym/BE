package com.skalez.member.oauth;

import com.skalez.global.error.BusinessException;
import com.skalez.global.error.ErrorCode;
import com.skalez.global.i18n.SupportedLanguage;
import com.skalez.global.mail.SiteProperties;
import com.skalez.global.security.JwtTokenProvider;
import com.skalez.member.api.AuthDtos.TokenResponse;
import com.skalez.member.domain.Member;
import com.skalez.member.domain.MemberSocialAccount;
import com.skalez.member.domain.SocialProvider;
import com.skalez.member.domain.SocialSignupTicket;
import com.skalez.member.repository.MemberRepository;
import com.skalez.member.repository.SocialAccountRepository;
import com.skalez.member.repository.SocialSignupTicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

/**
 * 카카오·네이버 간편 로그인.
 * 1) 이미 이어진 간편 로그인 계정 → 그 회원으로 로그인
 * 2) 처음이지만 제공자가 확인한 이메일이 기존 회원(아임웹 이전 회원 포함)과 같음 → 그 회원에 잇고 로그인
 * 3) 처음 온 사람 → 가입 표를 주고, 약관 동의(signup)를 받은 뒤 회원을 만든다
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SocialAuthService {

    static final Duration TICKET_TTL = Duration.ofMinutes(10);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OAuthClients clients;
    private final MemberRepository memberRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final SocialSignupTicketRepository ticketRepository;
    private final JwtTokenProvider tokenProvider;
    private final SiteProperties site;

    /** 로그인 결과: 바로 로그인(token) 또는 약관 동의가 필요한 가입(ticket) */
    public record Result(TokenResponse token, String ticket, String email, String name) {
        static Result login(TokenResponse token) {
            return new Result(token, null, null, null);
        }
    }

    public String authorizeUrl(SocialProvider provider, String redirectUri, String state) {
        checkRedirect(provider, redirectUri);
        return clients.get(provider).authorizeUrl(redirectUri, state);
    }

    @Transactional
    public Result login(SocialProvider provider, String code, String redirectUri, String state) {
        checkRedirect(provider, redirectUri);
        OAuthClient.Profile profile;
        try {
            profile = clients.get(provider).fetchProfile(code, redirectUri, state);
        } catch (RestClientException | IllegalStateException e) {
            log.warn("간편 로그인 실패 provider={}: {}", provider, e.getMessage());
            throw new BusinessException(ErrorCode.SOCIAL_LOGIN_FAILED);
        }
        if (profile.email() == null || profile.email().isBlank()) {
            throw new BusinessException(ErrorCode.SOCIAL_EMAIL_REQUIRED);
        }
        String email = profile.email().trim().toLowerCase(Locale.ROOT);

        // 1) 이미 이어진 계정
        var linked = socialAccountRepository.findByProviderAndProviderUserId(provider, profile.providerUserId());
        if (linked.isPresent()) {
            return Result.login(issue(memberRepository.findById(linked.get().getMemberId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND))));
        }

        // 2) 같은 이메일의 기존 회원: 제공자가 확인한 이메일일 때만 잇는다 (남의 이메일로 가입한 카카오 계정이 기존 회원을 가로채지 못하게)
        var sameEmail = memberRepository.findByEmail(email);
        if (sameEmail.isPresent()) {
            if (!profile.emailVerified()) throw new BusinessException(ErrorCode.SOCIAL_EMAIL_IN_USE);
            Member member = sameEmail.get();
            if (!socialAccountRepository.existsByMemberIdAndProvider(member.getId(), provider)) {
                socialAccountRepository.save(new MemberSocialAccount(member.getId(), provider, profile.providerUserId()));
            }
            return Result.login(issue(member));
        }

        // 3) 처음 온 사람: 약관 동의를 받을 가입 표
        String ticket = randomToken();
        ticketRepository.save(new SocialSignupTicket(hash(ticket), provider, profile.providerUserId(), email,
                profile.name(), Instant.now().plus(TICKET_TTL)));
        return new Result(null, ticket, email, profile.name());
    }

    /** 약관 동의를 받고 회원을 만든다. 가입 표는 한 번만 쓸 수 있다 */
    @Transactional
    public TokenResponse completeSignup(String ticket, String name, boolean marketingConsent, Locale locale) {
        Instant now = Instant.now();
        SocialSignupTicket found = ticketRepository.findByTicketHash(hash(ticket))
                .filter(item -> item.isUsable(now))
                .orElseThrow(() -> new BusinessException(ErrorCode.SOCIAL_SIGNUP_EXPIRED));
        found.markUsed(now);
        if (memberRepository.existsByEmail(found.getEmail())) throw new BusinessException(ErrorCode.EMAIL_DUPLICATED);
        if (socialAccountRepository.findByProviderAndProviderUserId(found.getProvider(), found.getProviderUserId()).isPresent()) {
            throw new BusinessException(ErrorCode.SOCIAL_SIGNUP_EXPIRED);
        }
        String memberName = name != null && !name.isBlank() ? name.trim() : found.getName() != null ? found.getName() : found.getEmail().substring(0, found.getEmail().indexOf('@'));
        Member member = memberRepository.save(Member.signup(found.getEmail(), null, memberName, SupportedLanguage.from(locale).tag(), marketingConsent));
        socialAccountRepository.save(new MemberSocialAccount(member.getId(), found.getProvider(), found.getProviderUserId()));
        return issue(member);
    }

    public boolean enabled(SocialProvider provider) {
        return clients.enabled(provider);
    }

    /** 로그인 창이 코드를 돌려보낼 주소는 우리 사이트의 콜백만 허용 (다른 곳으로 코드가 새지 않게) */
    private void checkRedirect(SocialProvider provider, String redirectUri) {
        String expected = site.url() + "/api/auth/oauth/" + provider.path() + "/callback";
        if (!expected.equals(redirectUri)) throw new BusinessException(ErrorCode.INVALID_INPUT);
    }

    private TokenResponse issue(Member member) {
        if (!member.isActive()) throw new BusinessException(ErrorCode.MEMBER_SUSPENDED);
        return new TokenResponse(tokenProvider.createAccessToken(member), "Bearer", tokenProvider.accessTokenTtlSeconds());
    }

    private static String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
