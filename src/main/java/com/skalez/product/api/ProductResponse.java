package com.skalez.product.api;

import com.skalez.global.i18n.SupportedLanguage;
import com.skalez.global.money.Money;
import com.skalez.product.domain.Badge;
import com.skalez.product.domain.Product;
import com.skalez.product.domain.ProductTranslation;
import com.skalez.product.domain.ProductVariant;

import java.time.Instant;
import java.util.List;

/** 가격은 최소 단위 정수 (KRW=원, USD=센트). FE에서 통화에 맞게 표시한다 */
public record ProductResponse(
        Long id,
        String sku,
        String categoryCode,
        String name,
        String description,
        String imageUrl,
        long price,
        Long listPrice,
        String currency,
        List<Badge> badges,
        boolean soldOut,
        List<VariantResponse> variants,
        String fit,
        String fabric,
        Instant createdAt
) {
    public record VariantResponse(Long id, String size, int stock) {
        static VariantResponse from(ProductVariant variant) {
            return new VariantResponse(variant.getId(), variant.getSize(), variant.getStock());
        }
    }

    public static ProductResponse of(Product product, SupportedLanguage language) {
        String currency = language.currency();
        ProductTranslation translation = product.translationFor(language);
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getCategory().getCode(),
                translation.getName(),
                translation.getDescription(),
                product.getImageUrl(),
                product.priceIn(currency).getAmount(),
                product.listPriceIn(currency).map(Money::getAmount).orElse(null),
                currency,
                product.getBadges().stream().sorted().toList(),
                product.isSoldOut(),
                product.getVariants().stream().map(VariantResponse::from).toList(),
                product.getFit(),
                product.getFabric(),
                product.getCreatedAt()
        );
    }
}