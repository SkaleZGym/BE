package com.skalez.point.repository;

import com.skalez.point.domain.PointHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {
    Page<PointHistory> findByMemberIdOrderByIdDesc(Long memberId, Pageable pageable);
}
