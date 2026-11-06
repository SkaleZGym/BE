package com.skalez.member.service;

import com.skalez.global.error.BusinessException;
import com.skalez.global.error.ErrorCode;
import com.skalez.global.i18n.SupportedLanguage;
import com.skalez.global.security.JwtTokenProvider;
import com.skalez.global.security.LoginAttemptLimiter;
import com.skalez.global.security.Totp;
import com.skalez.member.api.AuthDtos.AdminLoginRequest;
import com.skalez.member.api.AuthDtos.AdminLoginResponse;
import com.skalez.member.api.AuthDtos.ChangePasswordRequest;
import com.skalez.member.api.AuthDtos.LoginRequest;
import com.skalez.member.api.AuthDtos.MemberResponse;
import com.skalez.member.api.AuthDtos.SignupRequest;
import com.skalez.member.api.AuthDtos.TokenResponse;
import com.skalez.member.api.AuthDtos.UpdateMeRequest;
import com.skalez.member.domain.Member;
import com.skalez.member.domain.Role;
import com.skalez.member.repository.MemberRepository;
import com.skalez.member.repository.SocialAccountRepository;
import com.skalez.cart.repository.CartItemRepository;
import com.skalez.wish.repository.WishItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final LoginAttemptLimiter loginAttemptLimiter;
    private final SocialAccountRepository socialAccountRepository;
    private final CartItemRepository cartItemRepository;
    private final WishItemRepository wishItemRepository;

    @Transactional
    public MemberResponse signup(SignupRequest request, Locale locale) {
        String email = request.email().toLowerCase(Locale.ROOT);
        if (memberRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATED);
        }
        Member member = Member.signup(
                email,
                passwordEncoder.encode(request.password()),
                request.name(),
                SupportedLanguage.from(locale).tag(),
                Boolean.TRUE.equals(request.marketingConsent())
        );
        return MemberResponse.from(memberRepository.save(member));
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        Member member = authenticate(request);
        return refreshTokenService.issueWithAccess(member);
    }

    /** 2단계 인증이 켜진 뒤 코드를 연달아 이만큼 틀리면 잠긴다 (다른 관리자가 초기화) */
    static final int OTP_MAX_FAILURES = 10;
    static final String OTP_ISSUER = "SKALEZ";

    /**
     * 관리자 페이지 로그인: 비밀번호 + OTP 앱 6자리. 일반 회원이면 이메일/비밀번호가 틀린 것과 같은 에러 (관리자 여부 노출 방지).
     * - 코드 없이 오면: OTP 가 켜져 있으면 CODE(코드 입력), 아직 없으면 SETUP(새 비밀값 → OTP 앱 등록)
     * - 코드와 함께 오면: 맞을 때만 관리자 토큰. 등록 중이면 이때 켜진다
     * 코드 틀림도 로그인 실패로 센다(15분 5번). 켜진 뒤 연달아 10번 틀리면 잠긴다 — 틀린 횟수는 에러가 나도 저장한다(noRollbackFor).
     * 로그인 유지(리프레시 토큰)는 없어서 1시간마다 다시 로그인
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public AdminLoginResponse adminLogin(AdminLoginRequest request) {
        String email = request.email().toLowerCase(Locale.ROOT);
        Member member = checkPassword(email, request.password());
        if (member.getRole() != Role.ADMIN) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (!member.isActive()) {
            throw new BusinessException(ErrorCode.MEMBER_SUSPENDED);
        }
        if (member.getOtpFailures() >= OTP_MAX_FAILURES) {
            throw new BusinessException(ErrorCode.ADMIN_OTP_LOCKED);
        }
        if (request.code() == null) {
            if (member.isOtpEnabled()) {
                return AdminLoginResponse.codeRequired();
            }
            String secret = Totp.newSecret();
            member.startOtpSetup(secret);
            return AdminLoginResponse.setup(secret, Totp.uri(OTP_ISSUER, member.getEmail(), secret));
        }
        long step = Totp.matchingStep(member.getOtpSecret(), request.code(), Instant.now());
        if (!member.useOtpStep(step)) {
            loginAttemptLimiter.recordFailure(email);
            throw new BusinessException(member.getOtpFailures() >= OTP_MAX_FAILURES ? ErrorCode.ADMIN_OTP_LOCKED : ErrorCode.ADMIN_OTP_INVALID);
        }
        loginAttemptLimiter.reset(email);
        return AdminLoginResponse.ok(tokenProvider.createAdminAccessToken(member), tokenProvider.adminAccessTokenTtlSeconds());
    }

    public MemberResponse me(Long memberId) {
        return memberRepository.findById(memberId)
                .map(MemberResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    @Transactional
    public MemberResponse updateMe(Long memberId, UpdateMeRequest request) {
        Member member = findMember(memberId);
        // 지원하지 않는 언어 코드면 INVALID_INPUT
        String language = SupportedLanguage.fromTag(request.language()).tag();
        member.changeProfile(request.name().trim(), language);
        if (request.marketingConsent() != null) member.changeMarketingConsent(request.marketingConsent());
        return MemberResponse.from(member);
    }

    /** 현재 비밀번호를 한 번 더 확인한다 (로그인된 기기를 잠깐 빌린 사람이 바꾸지 못하게) */
    @Transactional
    public void changePassword(Long memberId, ChangePasswordRequest request) {
        Member member = findMember(memberId);
        if (member.needsPasswordSetup() || !passwordEncoder.matches(request.currentPassword(), member.getPassword())) {
            throw new BusinessException(ErrorCode.CURRENT_PASSWORD_MISMATCH);
        }
        member.changePassword(passwordEncoder.encode(request.newPassword()));
        // 다른 기기에 남은 로그인 유지도 끊는다 (지금 기기는 화면에서 다시 로그인)
        refreshTokenService.revokeAll(member.getId());
    }

    /**
     * 회원 탈퇴(개인정보 삭제 요청). 비밀번호가 있는 계정은 현재 비밀번호를 확인한다.
     * 장바구니·위시리스트·간편 로그인 연결·로그인 유지 토큰은 지우고, 회원 행은 비식별화해 남긴다 (주문 기록 보관용)
     */
    @Transactional
    public void withdraw(Long memberId, String password) {
        Member member = findMember(memberId);
        if (member.getRole() == Role.ADMIN) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        if (!member.needsPasswordSetup() && (password == null || !passwordEncoder.matches(password, member.getPassword()))) {
            throw new BusinessException(ErrorCode.CURRENT_PASSWORD_MISMATCH);
        }
        socialAccountRepository.deleteAll(socialAccountRepository.findByMemberId(member.getId()));
        cartItemRepository.deleteAll(cartItemRepository.findByMemberIdOrderByCreatedAtAsc(member.getId()));
        wishItemRepository.deleteAll(wishItemRepository.findByMemberIdOrderByCreatedAtDesc(member.getId()));
        member.withdraw();
        // revokeAll 은 영속성 컨텍스트를 비우므로(flush 후 clear) 회원 변경 뒤에 맨 마지막으로 부른다
        refreshTokenService.revokeAll(member.getId());
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    /**
     * 이메일이 없든 비밀번호가 틀리든 같은 에러를 준다 (가입 여부 노출 방지). 정지된 계정은 로그인 불가.
     * 아임웹에서 옮겨 온 회원은 비밀번호가 없어서 같은 에러 → 화면이 '비밀번호 찾기'로 안내한다
     */
    private Member authenticate(LoginRequest request) {
        String email = request.email().toLowerCase(Locale.ROOT);
        Member member = checkPassword(email, request.password());
        loginAttemptLimiter.reset(email);
        if (!member.isActive()) {
            throw new BusinessException(ErrorCode.MEMBER_SUSPENDED);
        }
        return member;
    }

    /** 비밀번호만 확인한다. 실패 기록을 지우는 건 부르는 쪽 (관리자는 OTP 까지 맞힌 뒤에 지운다) */
    private Member checkPassword(String email, String password) {
        loginAttemptLimiter.check(email);
        return memberRepository.findByEmail(email)
                .filter(found -> !found.needsPasswordSetup() && passwordEncoder.matches(password, found.getPassword()))
                .orElseThrow(() -> {
                    loginAttemptLimiter.recordFailure(email);
                    return new BusinessException(ErrorCode.INVALID_CREDENTIALS);
                });
    }
}