package com.skalez.audit.repository;

import com.skalez.audit.domain.AdminAccessLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface AdminAccessLogRepository extends JpaRepository<AdminAccessLog, Long> {
    Page<AdminAccessLog> findAllByOrderByIdDesc(Pageable pageable);
    @Modifying
    @Query("delete from AdminAccessLog l where l.accessedAt < :before")
    int deleteOlderThan(@Param("before")Instant before);
}
