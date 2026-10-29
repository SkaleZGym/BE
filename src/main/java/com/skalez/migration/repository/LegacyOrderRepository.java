package com.skalez.migration.repository;

import com.skalez.migration.domain.LegacyOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LegacyOrderRepository extends JpaRepository<LegacyOrder, Long> {
    boolean existsBySourceAndLegacyOrderNumber(String source, String legacyOrderNumber);
    List<LegacyOrder> findByEmailOrderByOrderedAtDesc(String email);
}
