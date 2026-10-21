package com.skalez.global.i18n;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public enum SupportedLanguage {
    KO(Locale.KOREAN, "KRW"),
    EN(Locale.ENGLISH, "USD"),
    JA(Locale.JAPANESE, "USD"),
    ZH_CN(Locale.SIMPLIFIED_CHINESE, "USD");

    public static final SupportedLanguage DEFAULT = KO;

    private final Locale locale;
    private final String currency;

    SupportedLanguage(Locale locale, String currency) {
        this.locale = locale;
        this.currency = currency;
    }

    public Locale locale() {
        return locale;
    }

    /** DB 저장 및 FE와 주고받는 코드: ko, en, ja, zh-CN */
    public String tag() {
        return locale.toLanguageTag();
    }

    /** 언어별 결제 통화. 기존 사이트처럼 한국어는 원화, 그 외는 달러 */
    public String currency() {
        return currency;
    }

    public static SupportedLanguage from(Locale locale) {
        return Arrays.stream(values())
                .filter(lang -> lang.locale.equals(locale))
                .findFirst()
                .orElse(DEFAULT);
    }

    public static List<Locale> locales() {
        return Arrays.stream(values()).map(SupportedLanguage::locale).toList();
    }
}