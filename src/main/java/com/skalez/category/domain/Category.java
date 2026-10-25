package com.skalez.category.domain;

import com.skalez.global.error.BusinessException;
import com.skalez.global.error.ErrorCode;
import com.skalez.global.i18n.SupportedLanguage;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 상품 분류. 최상위(parent 없음) → 하위, 2단계까지만 쓴다.
 * 상품 목록 조회(ProductRepository.findByCategoryCode)가 상위 1단계까지만 보기 때문에 더 깊게 만들지 않는다.
 */
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
        changeParent(parent);
        this.sortOrder= sortOrder;
    }
    public Category name(SupportedLanguage language, String name){
        names.put(language.tag(), name);
        return this;
    }
    public String nameFor(SupportedLanguage language){return names.getOrDefault(language.tag(), names.get(SupportedLanguage.DEFAULT.tag()));}

    public boolean isTopLevel() {
        return parent == null;
    }

    /** 관리자 수정용. 코드를 바꾸면 FE가 상품 목록 조회에 쓰는 값도 같이 바뀌니 주의 */
    public void change(String code, Category parent, int sortOrder) {
        this.code = code;
        changeParent(parent);
        this.sortOrder = sortOrder;
    }

    /** 관리자 수정용: 언어별 이름을 요청 내용으로 통째로 바꾼다 */
    public void replaceNames(Map<SupportedLanguage, String> newNames) {
        names.clear();
        newNames.forEach((language, name) -> names.put(language.tag(), name));
    }

    /** 상위는 최상위 카테고리만 될 수 있고, 자기 자신은 안 된다 */
    private void changeParent(Category parent) {
        if (parent != null && (parent == this || Objects.equals(parent.getId(), id) || !parent.isTopLevel())) {
            throw new BusinessException(ErrorCode.CATEGORY_PARENT_INVALID);
        }
        this.parent = parent;
    }
}
