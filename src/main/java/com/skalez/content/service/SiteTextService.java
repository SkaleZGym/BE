package com.skalez.content.service;

import com.skalez.content.api.ContentDtos.AdminSiteTextResponse;
import com.skalez.content.api.ContentDtos.SiteTextSaveRequest;
import com.skalez.content.domain.SiteText;
import com.skalez.content.repository.SiteTextRepository;
import com.skalez.global.error.BusinessException;
import com.skalez.global.error.ErrorCode;
import com.skalez.global.i18n.SupportedLanguage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SiteTextService {

    /** 키 규칙: 소문자·숫자·점·하이픈·밑줄 (예: home.hero.headline) */
    private static final Pattern KEY_PATTERN = Pattern.compile("[a-z0-9][a-z0-9._-]{0,99}");

    private final SiteTextRepository siteTextRepository;

    /** 고객 화면: 현재 언어로 { 키: 값 } 한 번에. FE는 이 값으로 기본 문구를 덮어쓴다 */
    public Map<String, String> getTexts(Locale locale) {
        SupportedLanguage language = SupportedLanguage.from(locale);
        Map<String, String> result = new TreeMap<>();
        siteTextRepository.findAllByOrderByTextKeyAsc().forEach(text -> {
            String value = text.valueFor(language);
            if (value != null) {
                result.put(text.getTextKey(), value);
            }
        });
        return result;
    }

    public List<AdminSiteTextResponse> getAll() {
        return siteTextRepository.findAllByOrderByTextKeyAsc().stream()
                .map(AdminSiteTextResponse::from)
                .toList();
    }

    /** 있으면 수정, 없으면 새로 만든다 */
    @Transactional
    public AdminSiteTextResponse save(String key, SiteTextSaveRequest request) {
        if (!KEY_PATTERN.matcher(key).matches() || !request.values().containsKey(SupportedLanguage.DEFAULT.tag())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        Map<SupportedLanguage, String> values = new EnumMap<>(SupportedLanguage.class);
        request.values().forEach((tag, value) -> values.put(SupportedLanguage.fromTag(tag), value));

        SiteText siteText = siteTextRepository.findByTextKey(key)
                .orElseGet(() -> siteTextRepository.save(new SiteText(key)));
        siteText.change(request.description(), values);
        return AdminSiteTextResponse.from(siteText);
    }

    @Transactional
    public void delete(String key) {
        siteTextRepository.findByTextKey(key).ifPresent(siteTextRepository::delete);
    }
}