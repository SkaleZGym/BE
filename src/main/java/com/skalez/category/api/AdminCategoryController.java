package com.skalez.category.api;

import com.skalez.category.api.AdminCategoryDtos.AdminCategoryResponse;
import com.skalez.category.api.AdminCategoryDtos.CategorySaveRequest;
import com.skalez.category.service.AdminCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 관리자 카테고리 관리. /api/admin/** 은 SecurityConfig에서 ADMIN만 통과시킨다 */
@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final AdminCategoryService adminCategoryService;

    /** 전체 카테고리를 정렬 순서대로 (상위·하위 섞여 나오고 parentCode로 구분) */
    @GetMapping
    public List<AdminCategoryResponse> getCategories() {
        return adminCategoryService.getCategories();
    }

    @GetMapping("/{categoryId}")
    public AdminCategoryResponse getCategory(@PathVariable Long categoryId) {
        return adminCategoryService.getCategory(categoryId);
    }

    /** 예: {"code":"MEN_TOP","parentCode":"MEN","sortOrder":2,"names":{"ko":"상의","en":"Tops"}} */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminCategoryResponse create(@Valid @RequestBody CategorySaveRequest request) {
        return adminCategoryService.create(request);
    }

    @PutMapping("/{categoryId}")
    public AdminCategoryResponse update(@PathVariable Long categoryId, @Valid @RequestBody CategorySaveRequest request) {
        return adminCategoryService.update(categoryId, request);
    }

    /** 상품이나 하위 카테고리가 있으면 409 CATEGORY_IN_USE. 먼저 상품을 다른 카테고리로 옮긴다 */
    @DeleteMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long categoryId) {
        adminCategoryService.delete(categoryId);
    }
}
