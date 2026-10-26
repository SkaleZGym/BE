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

/** 자주 묻는 질문. 언어별 질문(title)·답변(content), 분류별 노출 순서 */
@Entity
@Table(name = "faqs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Faq {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR) // enum을 varchar로 저장. H2·PostgreSQL 모두 같은 타입이라 Flyway 스키마 하나로 검증된다
    @Column(nullable = false, length = 20)
    private FaqCategory category;
    @Column(nullable = false)
    private int sortOrder;
    @Column(nullable = false)
    private boolean published;
    @ElementCollection
    @CollectionTable(name = "faq_texts", joinColumns = @JoinColumn(name = "faq_id"))
    @MapKeyColumn(name = "language", length = 10)
    private Map<String, BoardText> texts = new HashMap<>();
    @Column(nullable = false)
    private Instant updatedAt;
    public Faq(FaqCategory category) {
        this.category = category;
        this.updatedAt = Instant.now();
    }
    public void change(FaqCategory category, int sortOrder, boolean published, Map<SupportedLanguage, BoardText> newTexts) {
        this.category = category;
        this.sortOrder = sortOrder;
        this.published = published;
        texts.clear();
        newTexts.forEach((language, text) -> texts.put(language.tag(), text));
        this.updatedAt = Instant.now();
    }
    public BoardText textFor(SupportedLanguage language) {
        return texts.getOrDefault(language.tag(), texts.get(SupportedLanguage.DEFAULT.tag()));
    }
}