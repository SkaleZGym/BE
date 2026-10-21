package com.skalez.global.config;

import com.skalez.global.i18n.SupportedLanguage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

@Configuration
public class LocaleConfig {
    @Bean
    public LocaleResolver localeResolver(){
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setSupportedLocales(SupportedLanguage.locales());
        resolver.setDefaultLocale(SupportedLanguage.DEFAULT.locale());
        return resolver;
    }
}
