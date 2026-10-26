package com.skalez.content.service;

import com.skalez.content.api.ContentDtos.AdminBannerResponse;
import com.skalez.content.api.ContentDtos.BannerResponse;
import com.skalez.content.api.ContentDtos.BannerSaveRequest;
import com.skalez.content.domain.Banner;
import com.skalez.content.domain.BannerPosition;
import com.skalez.content.domain.BannerText;
import com.skalez.content.repository.BannerRepository;
import com.skalez.global.error.BusinessException;
import com.skalez.global.error.ErrorCode;
import com.skalez.global.i18n.SupportedLanguage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BannerService {

    private final BannerRepository bannerRepository;

    /** 고객 화면: 켜져 있고 노출 기간 안인 배너만, 순서대로 */
    public List<BannerResponse> getVisibleBanners(BannerPosition position, Locale locale) {
        SupportedLanguage language = SupportedLanguage.from(locale);
        Instant now = Instant.now();
        return bannerRepository.findByPositionOrderBySortOrderAsc(position).stream()
                .filter(banner -> banner.isVisibleAt(now))
                .map(banner -> BannerResponse.of(banner, language))
                .toList();
    }

    public List<AdminBannerResponse> getAllBanners() {
        return bannerRepository.findAllByOrderByPositionAscSortOrderAsc().stream()
                .map(AdminBannerResponse::from)
                .toList();
    }

    public AdminBannerResponse getBanner(Long bannerId) {
        return AdminBannerResponse.from(find(bannerId));
    }

    @Transactional
    public AdminBannerResponse create(BannerSaveRequest request) {
        Banner banner = new Banner(request.position(), request.imageUrl());
        apply(banner, request);
        return AdminBannerResponse.from(bannerRepository.save(banner));
    }

    @Transactional
    public AdminBannerResponse update(Long bannerId, BannerSaveRequest request) {
        Banner banner = find(bannerId);
        apply(banner, request);
        return AdminBannerResponse.from(banner);
    }

    @Transactional
    public void delete(Long bannerId) {
        bannerRepository.delete(find(bannerId));
    }

    private void apply(Banner banner, BannerSaveRequest request) {
        if (!request.texts().containsKey(SupportedLanguage.DEFAULT.tag())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT); // 한국어 문구는 필수
        }
        banner.change(request.position(), request.imageUrl(), request.mobileImageUrl(), request.linkUrl(), request.target(),
                request.sortOrder(), request.active(), request.startAt(), request.endAt());

        Map<SupportedLanguage, BannerText> texts = new EnumMap<>(SupportedLanguage.class);
        request.texts().forEach((tag, text) ->
                texts.put(SupportedLanguage.fromTag(tag), new BannerText(text.headline(), text.subtext())));
        banner.replaceTexts(texts);
    }

    private Banner find(Long bannerId) {
        return bannerRepository.findById(bannerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BANNER_NOT_FOUND));
    }
}