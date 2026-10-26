package com.skalez.board.domain;

import com.skalez.global.i18n.SupportedLanguage;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/** 공지사항·이벤트. 언어별 제목·내용 (한국어 필수, 없는 언어는 한국어로 보여 준다) */
@Entity
@Table(name = "notices")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR) // enum을 varchar로 저장. H2·PostgreSQL 모두 같은 타입이라 Flyway 스키마 하나로 검증된다
    @Column(nullable = false, length = 10)
    private NoticeCategory category;
    @Column(nullable = false)
    private boolean pinned;
    @Column(nullable = false)
    private boolean published;
    @Column(nullable = false)
    private long viewCount;
    @ElementCollection
    @CollectionTable(name = "notice_texts", joinColumns = @JoinColumn(name = "notice_id"))
    @MapKeyColumn(name = "language", length = 10)
    private Map<String, BoardText> texts = new HashMap<>();
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;
    public Notice(NoticeCategory category) {
        this.category = category;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }
    public void change(NoticeCategory category, boolean pinned, boolean published, Map<SupportedLanguage, BoardText> newTexts) {
        this.category = category;
        this.pinned = pinned;
        this.published = published;
        texts.clear();
        newTexts.forEach((language, text) -> texts.put(language.tag(), text));
        this.updatedAt = Instant.now();
    }
    public void increaseViewCount() {
        this.viewCount++;
    }
    public BoardText textFor(SupportedLanguage language) {
        return texts.getOrDefault(language.tag(), texts.get(SupportedLanguage.DEFAULT.tag()));
    }
}