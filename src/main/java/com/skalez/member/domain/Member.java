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

    /** 아임웹에서 옮겨 온 회원 표시 */
    public static final String LEGACY_IMWEB = "IMWEB";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email; // 로그인 조회에 써야 해서 암호화하지 않음

    /**
     * BCrypt 단방향 해시. 원래 비밀번호로 되돌릴 수 없고, 로그인 때 matches()로만 비교한다.
     * 아임웹에서 옮겨 온 회원은 비밀번호를 가져올 수 없어서 null — 메일 링크로 처음 설정할 때까지 로그인할 수 없다
     */
    private String password;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, length = 255)
    private String name; // AES-256 암호화 저장

    @Convert(converter = EncryptedStringConverter.class)
    @Column(length = 255)
    private String phone; // AES-256 암호화 저장 (아임웹 이전 회원)

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

    /** 마케팅(이벤트·할인 소식) 수신 동의. 동의한 시각을 함께 남긴다 (2년마다 재확인 대상) */
    @Column(nullable = false)
    private boolean marketingConsent;
    private Instant marketingConsentAt;

    /** 이용약관·개인정보 수집 동의 시각 (새 사이트 가입자). 이전 회원은 아임웹에서 이미 동의 → null */
    private Instant termsAgreedAt;

    @Column(length = 20)
    private String legacySource; // null = 새 사이트 가입, IMWEB = 아임웹 이전
    @Column(length = 50)
    private String legacyId; // 아임웹 회원 번호 등 (대조용)

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

    /** 새 사이트 회원가입: 약관 동의 시각과 마케팅 동의를 함께 남긴다 */
    public static Member signup(String email, String encodedPassword, String name, String language, boolean marketingConsent) {
        Member member = new Member(email, encodedPassword, name, language);
        member.termsAgreedAt = member.createdAt;
        member.changeMarketingConsent(marketingConsent);
        return member;
    }

    /** 아임웹에서 옮겨 온 회원. 비밀번호 없이 만들고, 가입일은 아임웹 가입일을 그대로 쓴다 */
    public static Member importLegacy(String source, String legacyId, String email, String name, String phone,
                                      Instant joinedAt, boolean marketingConsent, Instant marketingConsentAt) {
        Member member = new Member(email, null, name, "ko");
        member.legacySource = source;
        member.legacyId = legacyId;
        member.phone = phone;
        if (joinedAt != null) member.createdAt = joinedAt;
        member.marketingConsent = marketingConsent;
        member.marketingConsentAt = marketingConsent ? (marketingConsentAt != null ? marketingConsentAt : member.createdAt) : null;
        return member;
    }

    public boolean isActive() {
        return status == MemberStatus.ACTIVE;
    }

    /** 아임웹 이전 회원이 아직 새 비밀번호를 정하지 않았다 */
    public boolean needsPasswordSetup() {
        return password == null;
    }

    public void changeStatus(MemberStatus status) {
        this.status = status;
    }

    public void changeRole(Role role) {
        this.role = role;
    }

    /** 마이페이지: 이름·기본 언어 */
    public void changeProfile(String name, String language) {
        this.name = name;
        this.language = language;
    }

    /** 동의 상태가 바뀔 때만 시각을 새로 찍는다 */
    public void changeMarketingConsent(boolean consent) {
        if (consent == this.marketingConsent && (consent ? marketingConsentAt != null : marketingConsentAt == null)) return;
        this.marketingConsent = consent;
        this.marketingConsentAt = consent ? Instant.now() : null;
    }

    /** 이미 인코딩된 비밀번호를 받는다 (평문을 엔티티에 넘기지 않는다) */
    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }
}
