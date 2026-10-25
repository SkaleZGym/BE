package com.skalez.category.repository;

import com.skalez.category.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByOrderBySortOrderAsc();
    Optional<Category>findByCode(String code);
    boolean existsByCode(String code);
    /** 하위 카테고리가 있는지 (삭제·이동 가능 여부 판단) */
    boolean existsByParent(Category parent);
}
