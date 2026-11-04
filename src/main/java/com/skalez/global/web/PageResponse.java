package com.skalez.global.web;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    /** 개인정보가 담긴 관리자 목록(회원·주문)은 한 번에 많이 가져가지 못하게 한 쪽 크기를 줄인다 (기본 상한 2000) */
    public static final int PERSONAL_DATA_MAX_SIZE = 100;

    public static Pageable capped(Pageable pageable) {
        return pageable.getPageSize() <= PERSONAL_DATA_MAX_SIZE
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), PERSONAL_DATA_MAX_SIZE, pageable.getSort());
    }

    public static <T> PageResponse<T> from(Page<T> page){
        return new PageResponse<>(page.getContent(),page.getNumber(),page.getSize(),page.getTotalElements(), page.getTotalPages());
    }
}
