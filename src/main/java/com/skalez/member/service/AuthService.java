package com.skalez.member.service;

import com.skalez.global.error.BusinessException;
import com.skalez.global.error.ErrorCode;
import com.skalez.global.i18n.SupportedLanguage;
import com.skalez.global.security.JwtTokenProvider;
import com.skalez.member.api.AuthDtos.LoginRequest;
import com.skalez.member.api.AuthDtos.MemberResponse;
import com.skalez.member.api.AuthDtos.SignupRequest;
import com.skalez.member.api.AuthDtos.TokenResponse;
import com.skalez.member.domain.Member;
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
        if (memberRepository.existsByEmail(email)) {throw new BusinessException(ErrorCode.EMAIL_DUPLICATED);}
        Member member = new Member(email, passwordEncoder.encode(request.password()), request.name(), SupportedLanguage.from(locale).tag());
        return MemberResponse.from(memberRepository.save(member));}
    public TokenResponse login(LoginRequest request) {Member member = memberRepository.findByEmail(request.email().toLowerCase(Locale.ROOT)).filter(found -> passwordEncoder.matches(request.password(), found.getPassword())).orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
        return new TokenResponse(tokenProvider.createAccessToken(member), "Bearer", tokenProvider.accessTokenTtlSeconds()
        );
    }
    public MemberResponse me(Long memberId) {return memberRepository.findById(memberId).map(MemberResponse::from).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}