package com.skalez.member.service;

import com.skalez.global.error.BusinessException;
import com.skalez.global.error.ErrorCode;
import com.skalez.global.security.JwtProperties;
import com.skalez.global.security.JwtTokenProvider;
import com.skalez.member.api.AuthDtos.TokenResponse;
import com.skalez.member.domain.Member;
import com.skalez.member.domain.RefreshToken;
import com.skalez.member.repository.MemberRepository;
import com.skalez.member.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
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

/**
 * 로그인 유지(리프레시 토큰).
 * - 로그인할 때 접근 토큰(2시간)과 함께 발급하고, 접근 토큰이 만료되면 FE가 이 토큰으로 새 한 쌍을 받는다
 * - 한 번 쓴 토큰은 바로 폐기(교체). 이미 폐기된 토큰이 다시 들어오면 도용으로 보고 그 회원의 토큰을 모두 끊는다
 * - DB에는 SHA-256 해시만 저장한다
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefreshTokenService {

    static final Duration DEFAULT_TTL = Duration.ofDays(30);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberRepository memberRepository;
    private final JwtTokenProvider tokenProvider;
    private final JwtProperties jwtProperties;

    /** 로그인 성공: 접근 토큰 + 리프레시 토큰 */
    @Transactional
    public TokenResponse issueWithAccess(Member member) {
        Duration ttl = ttl();
        String token = newToken();
        refreshTokenRepository.save(new RefreshToken(member.getId(), hash(token), Instant.now().plus(ttl)));
        return new TokenResponse(tokenProvider.createAccessToken(member), "Bearer",
                tokenProvider.accessTokenTtlSeconds(), token, ttl.toSeconds());
    }

    /** 리프레시 토큰으로 새 한 쌍 발급 (쓴 토큰은 폐기) */
    @Transactional(noRollbackFor = BusinessException.class)
    public TokenResponse rotate(String token) {
        Instant now = Instant.now();
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(token))
                .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID));
        if (stored.isRevoked()) {
            // 이미 쓴 토큰이 또 왔다 = 누군가 복사해 간 토큰일 수 있다 → 그 회원의 로그인 유지를 모두 끊는다
            log.warn("폐기된 리프레시 토큰 재사용: 회원 {}", stored.getMemberId());
            refreshTokenRepository.revokeAll(stored.getMemberId(), now);
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        if (stored.isExpired(now)) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        Member member = memberRepository.findById(stored.getMemberId())
                .filter(Member::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID));
        stored.revoke(now);
        return issueWithAccess(member);
    }

    /** 로그아웃 */
    @Transactional
    public void revoke(String token) {
        refreshTokenRepository.findByTokenHash(hash(token)).ifPresent(stored -> stored.revoke(Instant.now()));
    }

    /** 비밀번호 변경·재설정: 모든 기기의 로그인 유지를 끊는다 */
    @Transactional
    public void revokeAll(Long memberId) {
        refreshTokenRepository.revokeAll(memberId, Instant.now());
    }

    /** 매일 새벽 기간이 지난 토큰을 지운다 */
    @Scheduled(cron = "0 30 4 * * *", zone = "Asia/Seoul")
    @Transactional
    public void deleteExpired() {
        int deleted = refreshTokenRepository.deleteExpired(Instant.now());
        if (deleted > 0) {
            log.info("기간 지난 리프레시 토큰 정리: {}개", deleted);
        }
    }

    private Duration ttl() {
        return jwtProperties.refreshTokenTtl() == null ? DEFAULT_TTL : jwtProperties.refreshTokenTtl();
    }

    private static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
