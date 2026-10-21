package com.skalez.category.api;

import com.skalez.category.domain.Category;
import com.skalez.global.i18n.SupportedLanguage;

public record CategoryResponse(Long id, String code, String parentCode, String name) {

    public static CategoryResponse of(Category category, SupportedLanguage language) {return new CategoryResponse(category.getId(), category.getCode(), category.getParent() == null ? null : category.getParent().getCode(), category.nameFor(language));}
}