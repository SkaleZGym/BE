package com.skalez.category.service;

import com.skalez.category.api.AdminCategoryDtos.AdminCategoryResponse;
import com.skalez.category.api.AdminCategoryDtos.CategorySaveRequest;
import com.skalez.category.domain.Category;
import com.skalez.category.repository.CategoryRepository;
import com.skalez.global.error.BusinessException;
import com.skalez.global.error.ErrorCode;
import com.skalez.global.i18n.SupportedLanguage;
import com.skalez.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminCategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public List<AdminCategoryResponse> getCategories() {
        return categoryRepository.findAllByOrderBySortOrderAsc().stream()
                .map(AdminCategoryResponse::from)
                .toList();
    }

    public AdminCategoryResponse getCategory(Long categoryId) {
        return AdminCategoryResponse.from(find(categoryId));
    }

    @Transactional
    public AdminCategoryResponse create(CategorySaveRequest request) {
        if (categoryRepository.existsByCode(request.code())) {
            throw new BusinessException(ErrorCode.CATEGORY_CODE_DUPLICATED);
        }
        Category category = new Category(request.code(), findParent(request.parentCode()), request.sortOrder());
        category.replaceNames(toNames(request.names()));
        return AdminCategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    public AdminCategoryResponse update(Long categoryId, CategorySaveRequest request) {
        Category category = find(categoryId);
        if (!category.getCode().equals(request.code()) && categoryRepository.existsByCode(request.code())) {
            throw new BusinessException(ErrorCode.CATEGORY_CODE_DUPLICATED);
        }
        Category parent = findParent(request.parentCode());
        // 하위 카테고리를 가진 카테고리를 다른 카테고리 아래로 옮기면 3단계가 되므로 막는다
        if (parent != null && categoryRepository.existsByParent(category)) {
            throw new BusinessException(ErrorCode.CATEGORY_PARENT_INVALID);
        }
        category.change(request.code(), parent, request.sortOrder());
        category.replaceNames(toNames(request.names()));
        return AdminCategoryResponse.from(category);
    }

    /** 상품(숨긴 상품 포함)이나 하위 카테고리가 없을 때만 지운다. 상품이 있으면 먼저 다른 카테고리로 옮겨야 한다 */
    @Transactional
    public void delete(Long categoryId) {
        Category category = find(categoryId);
        if (categoryRepository.existsByParent(category) || productRepository.existsByCategory(category)) {
            throw new BusinessException(ErrorCode.CATEGORY_IN_USE);
        }
        categoryRepository.delete(category);
    }

    /** 언어 코드 확인 + 한국어 이름 필수 */
    private Map<SupportedLanguage, String> toNames(Map<String, String> names) {
        if (!names.containsKey(SupportedLanguage.DEFAULT.tag())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        Map<SupportedLanguage, String> result = new EnumMap<>(SupportedLanguage.class);
        names.forEach((tag, name) -> result.put(SupportedLanguage.fromTag(tag), name.trim()));
        return result;
    }

    private Category findParent(String parentCode) {
        if (parentCode == null || parentCode.isBlank()) {
            return null;
        }
        return categoryRepository.findByCode(parentCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
    }

    private Category find(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
    }
}
