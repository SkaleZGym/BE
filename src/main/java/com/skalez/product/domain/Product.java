package com.skalez.product.domain;

import com.skalez.category.domain.Category;
import com.skalez.global.error.BusinessException;
import com.skalez.global.error.ErrorCode;
import com.skalez.global.i18n.SupportedLanguage;
import com.skalez.global.money.Money;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status;

    @Column(length = 500)
    private String imageUrl;

    /** 시장(통화)별 가격. 기존 사이트처럼 KRW와 USD를 따로 정한다 */
    @ElementCollection
    @CollectionTable(name = "product_prices", joinColumns = @JoinColumn(name = "product_id"))
    @MapKeyColumn(name = "currency", length = 3)
    private Map<String, PriceTag> prices = new HashMap<>();

    @ElementCollection
    @CollectionTable(name = "product_badges", joinColumns = @JoinColumn(name = "product_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "badge", length = 20)
    private Set<Badge> badges = new HashSet<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductTranslation> translations = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ProductVariant> variants = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt; // Instant = UTC 기준 시각

    public Product(String sku, Category category, String imageUrl) {
        this.sku = sku;
        this.category = category;
        this.imageUrl = imageUrl;
        this.status = ProductStatus.ON_SALE;
        this.createdAt = Instant.now();
    }

    public void addTranslation(SupportedLanguage language, String name, String description) {
        translations.add(new ProductTranslation(this, language.tag(), name, description));
    }

    /** 가격은 최소 단위(원, 센트). listAmount는 할인 전 가격(없으면 null) */
    public void putPrice(String currency, long amount, Long listAmount) {
        Currency.getInstance(currency); // 잘못된 통화코드 방지
        prices.put(currency, new PriceTag(amount, listAmount));
    }

    public ProductVariant addVariant(String size, int stock) {
        ProductVariant variant = new ProductVariant(this, size, stock, variants.size());
        variants.add(variant);
        return variant;
    }

    public void addBadge(Badge badge) {
        badges.add(badge);
    }

    public Money priceIn(String currency) {
        return Money.of(priceTagIn(currency).getAmount(), currency);
    }

    public Optional<Money> listPriceIn(String currency) {
        return Optional.ofNullable(priceTagIn(currency).getListAmount())
                .map(amount -> Money.of(amount, currency));
    }

    public boolean isSoldOut() {
        return variants.stream().allMatch(variant -> variant.getStock() == 0);
    }

    /** 요청 언어 번역 → 없으면 한국어 번역 */
    public ProductTranslation translationFor(SupportedLanguage language) {
        return findTranslation(language)
                .or(() -> findTranslation(SupportedLanguage.DEFAULT))
                .orElseThrow(() -> new IllegalStateException("기본 번역이 없는 상품입니다: " + sku));
    }

    private PriceTag priceTagIn(String currency) {
        PriceTag tag = prices.get(currency);
        if (tag == null) {
            throw new BusinessException(ErrorCode.PRICE_NOT_AVAILABLE);
        }
        return tag;
    }

    private Optional<ProductTranslation> findTranslation(SupportedLanguage language) {
        return translations.stream()
                .filter(t -> t.getLanguage().equals(language.tag()))
                .findFirst();
    }
}