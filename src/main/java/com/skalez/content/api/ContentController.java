package com.skalez.content.api;

import com.skalez.content.api.ContentDtos.BannerResponse;
import com.skalez.content.domain.BannerPosition;
import com.skalez.content.service.BannerService;
import com.skalez.content.service.SiteTextService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.Map;
@RestController
@RequiredArgsConstructor
public class ContentController {
    private final BannerService bannerService;
    private final SiteTextService siteTextService;
    @GetMapping("/api/banners")
    public List<BannerResponse> getBanners(@RequestParam BannerPosition position, Locale locale){return bannerService.getVisibleBanners(position, locale);}
    @GetMapping("/api/site-texts")
    public Map<String, String> getSiteTexts(Locale locale){return siteTextService.getTexts(locale);}
}
