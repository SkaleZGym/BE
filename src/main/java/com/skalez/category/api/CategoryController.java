package com.skalez.category.api;

import com.skalez.category.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

@RestController
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    @GetMapping("/api/categories")
    public List<CategoryResponse> getCategories(Locale locale) {return categoryService.getCategories(locale);}
}