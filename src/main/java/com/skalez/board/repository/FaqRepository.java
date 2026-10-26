package com.skalez.board.repository;

import com.skalez.board.domain.Faq;
import com.skalez.board.domain.FaqCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FaqRepository extends JpaRepository<Faq, Long> {
    List<Faq> findByPublishedTrueOrderByCategoryAscSortOrderAsc();
    List<Faq> findByPublishedTrueAndCategoryOrderBySortOrderAsc(FaqCategory category);
    List<Faq> findAllByOrderByCategoryAscSortOrderAsc();
}
