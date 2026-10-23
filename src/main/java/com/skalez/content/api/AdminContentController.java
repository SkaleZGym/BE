package com.skalez.content.api;

import com.skalez.content.api.ContentDtos.AdminBannerResponse;
import com.skalez.content.api.ContentDtos.AdminSiteTextResponse;
import com.skalez.content.api.ContentDtos.BannerSaveRequest;
import com.skalez.content.api.ContentDtos.SiteTextSaveRequest;
import com.skalez.content.service.BannerService;
import com.skalez.content.service.SiteTextService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminContentController {
    private final BannerService bannerService;
    private final SiteTextService siteTextService;
    //==배너====
    @GetMapping("/banners")
    public List<AdminBannerResponse> getBanners(){return bannerService.getAllBanners();}
    @GetMapping("/banners/{bannerId}")
    public AdminBannerResponse getBanner(@PathVariable Long bannerId){return bannerService.getBanner(bannerId);}
    @PostMapping("/banners")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminBannerResponse createBanner(@Valid @RequestBody BannerSaveRequest request){return bannerService.create(request);}
    @PutMapping("/banners/{bannerId}")
    public AdminBannerResponse updateBanner(@PathVariable Long bannerId, @Valid @RequestBody BannerSaveRequest request){return bannerService.update(bannerId, request);}
    @DeleteMapping("/banners/{bannerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBanner(@PathVariable Long bannerId){bannerService.delete(bannerId);}
    //==사이트 문구===
     @GetMapping("/site-texts")
    public List<AdminSiteTextResponse> getSiteTexts(){return siteTextService.getAll();}
    @PutMapping("/site-texts/{key}")
    public AdminSiteTextResponse saveSiteText(@PathVariable String key, @Valid @RequestBody SiteTextSaveRequest request){return siteTextService.save(key, request);}
    @DeleteMapping("/site-texts/{key}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSiteText(@PathVariable String key){siteTextService.delete(key);}
}
