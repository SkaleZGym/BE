package com.skalez.member.domain;

import com.skalez.global.security.crypto.EncryptedStringConverter;
import jakarta.persistence.*;
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

    @Column(nullable = false)
    private String password;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, length = 255)
    private String name; // AES-256 암호화 저장

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

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
        this.createdAt = Instant.now();
    }
}