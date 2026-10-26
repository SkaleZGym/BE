package com.skalez.member.service;

import com.skalez.global.error.BusinessException;
import com.skalez.global.error.ErrorCode;
import com.skalez.global.i18n.SupportedLanguage;
import com.skalez.global.security.JwtTokenProvider;
import com.skalez.member.api.AuthDtos.ChangePasswordRequest;
import com.skalez.member.api.AuthDtos.LoginRequest;
import com.skalez.member.api.AuthDtos.MemberResponse;
import com.skalez.member.api.AuthDtos.SignupRequest;
import com.skalez.member.api.AuthDtos.TokenResponse;
import com.skalez.member.api.AuthDtos.UpdateMeRequest;
import com.skalez.member.domain.Member;
import com.skalez.member.domain.Role;
import com.skalez.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

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

    public TokenResponse login(LoginRequest request) {
        return issueToken(authenticate(request));
    }

    /** 관리자 페이지 로그인. 일반 회원이면 이메일/비밀번호가 틀린 것과 같은 에러 (관리자 여부 노출 방지) */
    public TokenResponse adminLogin(LoginRequest request) {
        Member member = authenticate(request);
        if (member.getRole() != Role.ADMIN) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        return issueToken(member);
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
        Member member = memberRepository.findByEmail(request.email().toLowerCase(Locale.ROOT))
                .filter(found -> !found.needsPasswordSetup() && passwordEncoder.matches(request.password(), found.getPassword()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
        if (!member.isActive()) {
            throw new BusinessException(ErrorCode.MEMBER_SUSPENDED);
        }
        return member;
    }

    private TokenResponse issueToken(Member member) {
        return new TokenResponse(
                tokenProvider.createAccessToken(member),
                "Bearer",
                tokenProvider.accessTokenTtlSeconds()
        );
    }
}