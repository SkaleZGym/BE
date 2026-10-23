package com.skalez.content.repository;

import com.skalez.content.domain.SiteText;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SiteTextRepository extends JpaRepository<SiteText, Long> {
    Optional<SiteText> findByTextKey(String textKey);
    List<SiteText> findAllByOrderByTextKeyAsc();
}
