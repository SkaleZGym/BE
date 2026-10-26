package com.skalez.member.domain;

import com.skalez.global.security.crypto.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/** 간편 로그인으로 처음 온 사람이 약관에 동의할 때까지 들고 있는 가입 표 */
@Entity
@Table(name = "social_signup_tickets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialSignupTicket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 64, unique = true)
    private String ticketHash;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR) // enum을 varchar로 저장. H2·PostgreSQL 모두 같은 타입이라 Flyway 스키마 하나로 검증된다
    @Column(nullable = false, length = 20)
    private SocialProvider provider;
    @Column(nullable = false, length = 100)
    private String providerUserId;
    @Column(nullable = false, length = 100)
    private String email;
    @Convert(converter = EncryptedStringConverter.class)
    @Column(length = 255)
    private String name;
    @Column(nullable = false)
    private Instant expiresAt;
    private Instant usedAt;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public SocialSignupTicket(String ticketHash, SocialProvider provider, String providerUserId, String email, String name, Instant expiresAt) {
        this.ticketHash = ticketHash;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.email = email;
        this.name = name;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }

    public boolean isUsable(Instant now) {
        return usedAt == null && now.isBefore(expiresAt);
    }

    public void markUsed(Instant now) {
        this.usedAt = now;
    }
}
