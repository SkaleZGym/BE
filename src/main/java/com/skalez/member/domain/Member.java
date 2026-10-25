package com.skalez.member.domain;

import com.skalez.global.security.crypto.EncryptedStringConverter;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email; // 로그인 조회에 써야 해서 암호화하지 않음

    /** BCrypt 단방향 해시. 원래 비밀번호로 되돌릴 수 없고, 로그인 때 matches()로만 비교한다 */
    @Column(nullable = false)
    private String password;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, length = 255)
    private String name; // AES-256 암호화 저장

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR) // enum을 varchar로 저장. H2·PostgreSQL 모두 같은 타입이라 Flyway 스키마 하나로 검증된다
    @Column(nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR) // enum을 varchar로 저장. H2·PostgreSQL 모두 같은 타입이라 Flyway 스키마 하나로 검증된다
    @Column(nullable = false, length = 20)
    private MemberStatus status;

    @Column(nullable = false, length = 10)
    private String language; // 가입 시 언어: ko, en, ja, zh-CN

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public Member(String email, String encodedPassword, String name, String language) {
        this.email = email;
        this.password = encodedPassword;
        this.name = name;
        this.language = language;
        this.role = Role.USER;
        this.status = MemberStatus.ACTIVE;
        this.createdAt = Instant.now();
    }

    public boolean isActive() {
        return status == MemberStatus.ACTIVE;
    }

    public void changeStatus(MemberStatus status) {
        this.status = status;
    }

    public void changeRole(Role role) {
        this.role = role;
    }
}