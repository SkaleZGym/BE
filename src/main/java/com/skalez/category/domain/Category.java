package com.skalez.category.domain;

import com.skalez.global.i18n.SupportedLanguage;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 50)
    private String code;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;
    @Column(nullable = false)
    private int sortOrder;
    @ElementCollection
    @CollectionTable(name = "category_names", joinColumns = @JoinColumn(name = "category_id"))
    @MapKeyColumn(name = "language", length = 10)
    @Column(name = "name", nullable = false, length = 100)
    private Map<String, String> names = new HashMap<>();
    public Category(String code, Category parent, int sortOrder){
        this.code = code;
        this.parent = parent;
        this.sortOrder= sortOrder;
    }
    public Category name(SupportedLanguage language, String name){
        names.put(language.tag(), name);
        return this;
    }
    public String nameFor(SupportedLanguage language){return names.getOrDefault(language.tag(), names.get(SupportedLanguage.DEFAULT.tag()));}
}
