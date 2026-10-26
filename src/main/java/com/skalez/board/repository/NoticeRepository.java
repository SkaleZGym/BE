package com.skalez.board.repository;

import com.skalez.board.domain.Notice;
import com.skalez.board.domain.NoticeCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    /** 고객 목록: 고정 글 먼저, 그다음 최신순 */
    Page<Notice> findByPublishedTrueOrderByPinnedDescCreatedAtDesc(Pageable pageable);
    Page<Notice> findByPublishedTrueAndCategoryOrderByPinnedDescCreatedAtDesc(NoticeCategory category, Pageable pageable);
    Optional<Notice> findByIdAndPublishedTrue(Long id);
}
