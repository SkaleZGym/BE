package com.skalez.content.domain;

import com.skalez.global.i18n.SupportedLanguage;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "site_texts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SiteText {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "text_key", nullable = false, unique = true, length = 100) // key는 SQL 예약어라 text_key
    private String textKey;
    @Column(length = 200)
    private String description;
    @ElementCollection
    @CollectionTable(name = "site_text_values", joinColumns = @JoinColumn(name = "site_text_id"))
    @MapKeyColumn(name = "language", length = 10)
    @Column(name = "text_value", length = 2000)
    private Map<String, String> values = new HashMap<>();
    @Column(nullable = false)
    private Instant updatedAt;
    public SiteText(String textKey) {
        this.textKey = textKey;
        this.updatedAt = Instant.now();
    }
    public void change(String description, Map<SupportedLanguage, String> newValues) {
        this.description = description;
        values.clear();
        newValues.forEach((language, value) -> values.put(language.tag(), value));
        this.updatedAt = Instant.now();
    }
    public String valueFor(SupportedLanguage language) {return values.getOrDefault(language.tag(), values.get(SupportedLanguage.DEFAULT.tag()));}
}