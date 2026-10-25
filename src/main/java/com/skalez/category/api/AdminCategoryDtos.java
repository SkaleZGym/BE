package com.skalez.category.api;

import com.skalez.category.domain.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.TreeMap;

/** 관리자 카테고리 등록·수정·조회 요청·응답. 언어 키는 ko, en, ja, zh-CN */
public final class AdminCategoryDtos {

    private AdminCategoryDtos() {
    }

    /**
     * 카테고리 등록·수정 공통 요청.
     * code: 대문자로 시작, 대문자·숫자·밑줄 (예: MEN, MEN_TOP). FE가 상품 목록 조회(?category=)에 그대로 쓰는 값
     * parentCode: 상위 카테고리 코드. 최상위면 비운다. 상위는 최상위 카테고리만 될 수 있다 (2단계까지)
     * names: 언어별 이름. ko는 필수
     */
    public record CategorySaveRequest(
            @NotBlank @Pattern(regexp = "[A-Z][A-Z0-9_]{0,49}") String code,
            @Size(max = 50) String parentCode,
            int sortOrder,
            @NotEmpty Map<String, @NotBlank @Size(max = 100) String> names
    ) {
    }

    public record AdminCategoryResponse(Long id, String code, String parentCode, int sortOrder, Map<String, String> names) {
        public static AdminCategoryResponse from(Category category) {
            return new AdminCategoryResponse(
                    category.getId(),
                    category.getCode(),
                    category.getParent() == null ? null : category.getParent().getCode(),
                    category.getSortOrder(),
                    new TreeMap<>(category.getNames())
            );
        }
    }
}
