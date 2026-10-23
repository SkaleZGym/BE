package com.skalez.product.api;

import com.skalez.product.domain.Badge;
import com.skalez.product.domain.Product;
import com.skalez.product.domain.ProductStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.*;

public final class AdminProductDtos {

    private AdminProductDtos() {}
    public record TranslationDto(@NotBlank @Size(max = 200) String name, @Size(max = 2000) String description) {}
    public record PriceDto(@PositiveOrZero long amount, @PositiveOrZero Long listAmount) { }
    public record VariantRequest(@NotBlank @Size(max = 10) String size, @PositiveOrZero int stock) { }

    /**
     * 상품 등록·수정 공통 요청.
     * translations 키: ko, en, ja, zh-CN (ko는 필수)
     * prices 키: KRW, USD
     */
    public record ProductSaveRequest(
            @NotBlank @Size(max = 50) String sku,
            @NotBlank String categoryCode,
            @Size(max = 500) String imageUrl,
            @NotNull ProductStatus status,
            @NotEmpty Map<String, @Valid TranslationDto> translations,
            @NotEmpty Map<@Pattern(regexp = "[A-Z]{3}") String, @Valid PriceDto> prices,
            @NotEmpty List<@Valid VariantRequest> variants,
            Set<Badge> badges
    ) {
    }

    public record ChangeStatusRequest(@NotNull ProductStatus status) { }
    public record ChangeStockRequest(@PositiveOrZero int stock) { }
    public record VariantResponse(Long id, String size, int stock) { }

    public record AdminProductResponse(
            Long id,
            String sku,
            String categoryCode,
            String imageUrl,
            String status,
            Map<String, TranslationDto> translations,
            Map<String, PriceDto> prices,
            List<VariantResponse> variants,
            List<Badge> badges,
            Instant createdAt
    ) {
        public static AdminProductResponse from(Product product) {
            Map<String, TranslationDto> translations = new TreeMap<>();
            product.getTranslations().forEach(t ->
                    translations.put(t.getLanguage(), new TranslationDto(t.getName(), t.getDescription())));

            Map<String, PriceDto> prices = new TreeMap<>();
            product.getPrices().forEach((currency, tag) ->
                    prices.put(currency, new PriceDto(tag.getAmount(), tag.getListAmount())));

            return new AdminProductResponse(
                    product.getId(),
                    product.getSku(),
                    product.getCategory().getCode(),
                    product.getImageUrl(),
                    product.getStatus().name(),
                    translations,
                    prices,
                    product.getVariants().stream()
                            .map(v -> new VariantResponse(v.getId(), v.getSize(), v.getStock()))
                            .toList(),
                    product.getBadges().stream().sorted().toList(),
                    product.getCreatedAt()
            );
        }
    }
}