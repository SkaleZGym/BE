package com.skalez.member.service;

import com.skalez.global.error.BusinessException;
import com.skalez.global.error.ErrorCode;
import com.skalez.global.mail.MailClient;
import com.skalez.global.mail.SiteProperties;
import com.skalez.member.domain.Member;
import com.skalez.member.domain.PasswordResetToken;
import com.skalez.member.repository.MemberRepository;
import com.skalez.member.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
 * 비밀번호 재설정(비밀번호 찾기)과 아임웹 이전 회원의 첫 비밀번호 설정.
 * - 요청은 가입 여부와 상관없이 항상 같은 응답 (어떤 이메일이 회원인지 알려 주지 않는다)
 * - 링크 토큰은 32바이트 난수, DB에는 SHA-256 해시만 저장, 한 번 쓰면 끝
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PasswordResetService {

    /** 일반 재설정 링크는 30분, 이전 회원 첫 설정 링크는 넉넉히 24시간 */
    static final Duration RESET_TTL = Duration.ofMinutes(30);
    static final Duration SETUP_TTL = Duration.ofHours(24);
    /** 같은 회원에게 메일을 너무 자주 보내지 않게 (메일 폭탄 방지) */
    static final Duration RESEND_GAP = Duration.ofSeconds(60);

    private static final SecureRandom RANDOM = new SecureRandom();

    private final MemberRepository memberRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailClient mailClient;
    private final SiteProperties site;

    @Transactional
    public void request(String email) {
        Instant now = Instant.now();
        memberRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .filter(Member::isActive)
                .filter(member -> tokenRepository.findFirstByMemberIdOrderByCreatedAtDesc(member.getId())
                        .map(last -> last.getCreatedAt().plus(RESEND_GAP).isBefore(now))
                        .orElse(true))
                .ifPresent(member -> issue(member, now));
    }

    @Transactional
    public void confirm(String token, String newPassword) {
        Instant now = Instant.now();
        PasswordResetToken resetToken = tokenRepository.findByTokenHash(hash(token))
                .filter(found -> found.isUsable(now))
                .orElseThrow(() -> new BusinessException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID));
        Member member = memberRepository.findById(resetToken.getMemberId())
                .filter(Member::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID));
        member.changePassword(passwordEncoder.encode(newPassword));
        tokenRepository.expireAll(member.getId(), now);
    }

    private void issue(Member member, Instant now) {
        boolean setup = member.needsPasswordSetup() && member.getLegacySource() != null;
        Duration ttl = setup ? SETUP_TTL : RESET_TTL;
        String token = newToken();
        tokenRepository.save(new PasswordResetToken(member.getId(), hash(token), now.plus(ttl)));

        String link = site.url() + "/" + member.getLanguage() + "/reset-password?token=" + token;
        try {
            mailClient.send(PasswordMails.build(member.getEmail(), member.getLanguage(), setup, link, ttl));
        } catch (RuntimeException e) {
            // 메일 서버 오류를 고객 응답으로 드러내지 않는다 (가입 여부 노출 방지). 운영자는 로그로 확인
            log.error("비밀번호 메일 발송 실패 memberId={}", member.getId(), e);
        }
    }

    private static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
