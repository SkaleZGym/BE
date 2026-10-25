package com.skalez.content.domain;

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

@Entity
@Table(name = "banners")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Banner {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR) // enum을 varchar로 저장. H2·PostgreSQL 모두 같은 타입이라 Flyway 스키마 하나로 검증된다
    @Column(nullable = false, length = 20)
    private BannerPosition position;
    @Column(nullable = false, length = 500)
    private String imageUrl;
    @Column(length = 500)
    private String linkUrl;
    @Column(nullable = false)
    private int sortOrder;
    @Column(nullable = false)
    private boolean active;
    private Instant startAt;
    private Instant endAt;
    @ElementCollection
    @CollectionTable(name = "banner_texts", joinColumns = @JoinColumn(name = "banner_id"))
    @MapKeyColumn(name = "language", length = 10)
    private Map<String, BannerText> texts = new HashMap<>();
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    public Banner(BannerPosition position, String imageUrl){
        this.position = position;
        this.imageUrl = imageUrl;
        this.createdAt = Instant.now();
    }
    public void change(BannerPosition position, String imageUrl, String linkUrl, int sortOrder, boolean active, Instant startAt, Instant endAt){
        if (startAt != null && endAt != null && !endAt.isAfter(startAt)) {throw new IllegalArgumentException("종료 시각은 시작 시각보다 뒤여야 합니다");}
        this.position = position;
        this.imageUrl = imageUrl;
        this.linkUrl = linkUrl;
        this.sortOrder = sortOrder;
        this.active = active;
        this.startAt = startAt;
        this.endAt = endAt;
    }
    public void replaceTexts(Map<SupportedLanguage, BannerText>newTexts){
        texts.clear();
        newTexts.forEach((language, text) -> texts.put(language.tag(), text));
    }
    public boolean isVisibleAt(Instant now){
        return active && (startAt == null || !now.isBefore(startAt)) && (endAt == null || now.isBefore(endAt));
    }
    public BannerText textFor(SupportedLanguage language){return texts.getOrDefault(language.tag(), texts.get(SupportedLanguage.DEFAULT.tag()));}

}
