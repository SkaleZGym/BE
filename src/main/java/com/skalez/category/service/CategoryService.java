package com.skalez.category.service;

import com.skalez.category.api.CategoryResponse;
import com.skalez.category.repository.CategoryRepository;
import com.skalez.global.i18n.SupportedLanguage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryResponse> getCategories(Locale locale) {
        SupportedLanguage language = SupportedLanguage.from(locale);
        return categoryRepository.findAllByOrderBySortOrderAsc().stream().map(category -> CategoryResponse.of(category, language)).toList();
    }
}