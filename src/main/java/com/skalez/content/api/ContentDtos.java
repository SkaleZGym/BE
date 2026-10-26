package com.skalez.content.api;

import com.skalez.content.domain.Banner;
import com.skalez.content.domain.BannerPosition;
import com.skalez.content.domain.BannerTarget;
import com.skalez.content.domain.BannerText;
import com.skalez.content.domain.SiteText;
import com.skalez.global.i18n.SupportedLanguage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;

/** 배너·사이트 문구 요청·응답 모음. 언어 키는 ko, en, ja, zh-CN */
public final class ContentDtos {

    private ContentDtos() {
    }

    // ---------- 배너 ----------

    public record BannerTextDto(
            @Size(max = 200) String headline,
            @Size(max = 500) String subtext,
            /** 가격 문구 (선택) */
            @Size(max = 50) String price
    ) {
    }

    public record BannerSaveRequest(
            @NotNull BannerPosition position,
            @NotBlank @Size(max = 500) String imageUrl,
            @Size(max = 500) String mobileImageUrl,
            @Size(max = 500) String linkUrl,
            BannerTarget target,
            int sortOrder,
            boolean active,
            Instant startAt,
            Instant endAt,
            @NotEmpty Map<String, @Valid BannerTextDto> texts
    ) {
    }

    /** 고객 화면용: 현재 언어 문구만 */
    public record BannerResponse(Long id, String position, String imageUrl, String mobileImageUrl, String linkUrl,
                                 String target, String headline, String subtext, String price) {
        public static BannerResponse of(Banner banner, SupportedLanguage language) {
            BannerText text = banner.textFor(language);
            return new BannerResponse(
                    banner.getId(),
                    banner.getPosition().name(),
                    banner.getImageUrl(),
                    banner.getMobileImageUrl(),
                    banner.getLinkUrl(),
                    banner.getTarget().name(),
                    text == null ? null : text.getHeadline(),
                    text == null ? null : text.getSubtext(),
                    text == null ? null : text.getPrice()
            );
        }
    }

    /** 관리자용: 모든 언어 문구와 노출 설정 */
    public record AdminBannerResponse(
            Long id,
            String position,
            String imageUrl,
            String mobileImageUrl,
            String linkUrl,
            String target,
            int sortOrder,
            boolean active,
            boolean visibleNow,
            Instant startAt,
            Instant endAt,
            Map<String, BannerTextDto> texts
    ) {
        public static AdminBannerResponse from(Banner banner) {
            Map<String, BannerTextDto> texts = new TreeMap<>();
            banner.getTexts().forEach((tag, text) -> texts.put(tag, new BannerTextDto(text.getHeadline(), text.getSubtext(), text.getPrice())));
            return new AdminBannerResponse(
                    banner.getId(),
                    banner.getPosition().name(),
                    banner.getImageUrl(),
                    banner.getMobileImageUrl(),
                    banner.getLinkUrl(),
                    banner.getTarget().name(),
                    banner.getSortOrder(),
                    banner.isActive(),
                    banner.isVisibleAt(Instant.now()),
                    banner.getStartAt(),
                    banner.getEndAt(),
                    texts
            );
        }
    }

    // ---------- 사이트 문구 ----------

    public record SiteTextSaveRequest(
            @Size(max = 200) String description,
            @NotEmpty Map<String, @NotNull @Size(max = 2000) String> values
    ) {
    }

    public record AdminSiteTextResponse(String key, String description, Map<String, String> values, Instant updatedAt) {
        public static AdminSiteTextResponse from(SiteText siteText) {
            return new AdminSiteTextResponse(
                    siteText.getTextKey(),
                    siteText.getDescription(),
                    new TreeMap<>(siteText.getValues()),
                    siteText.getUpdatedAt()
            );
        }
    }
}