package com.skalez.global.security;

import com.skalez.member.domain.Member;
import com.skalez.member.domain.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final Duration DEFAULT_ADMIN_TTL = Duration.ofHours(1);

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;

    /**
     * 쇼핑몰 로그인 토큰. 관리자 계정이어도 USER 권한만 준다 —
     * 관리자 권한은 비밀번호 + OTP 를 거친 관리자 로그인(createAdminAccessToken)에서만 나온다
     */
    public String createAccessToken(Member member) {
        return create(member, Role.USER, properties.accessTokenTtl());
    }

    /** 관리자 페이지 토큰 (비밀번호 + OTP 확인 뒤). 쇼핑몰 토큰보다 짧다 */
    public String createAdminAccessToken(Member member) {
        return create(member, Role.ADMIN, adminTtl());
    }

    public long accessTokenTtlSeconds() {return properties.accessTokenTtl().toSeconds();}

    public long adminAccessTokenTtlSeconds() {return adminTtl().toSeconds();}

    private Duration adminTtl() {
        return properties.adminAccessTokenTtl() != null ? properties.adminAccessTokenTtl() : DEFAULT_ADMIN_TTL;
    }

    private String create(Member member, Role role, Duration ttl) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("skalez").subject(String.valueOf(member.getId())).issuedAt(now).expiresAt(now.plus(ttl)).claim("roles", List.of(role.name())).build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
