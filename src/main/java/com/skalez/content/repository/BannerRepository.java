package com.skalez.content.repository;

import com.skalez.content.domain.Banner;
import com.skalez.content.domain.BannerPosition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BannerRepository extends JpaRepository<Banner, Long> {
    List<Banner> findByPositionOrderBySortOrderAsc(BannerPosition position);
    List<Banner>findAllByOrderByPositionAscSortOrderAsc();
}
