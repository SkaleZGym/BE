package com.skalez.inquiry.repository;

import com.skalez.inquiry.domain.Inquiry;
import com.skalez.inquiry.domain.InquiryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
    Page<Inquiry> findByMemberId(Long memberId, Pageable pageable);
    Optional<Inquiry> findByIdAndMemberId(Long id, Long memberId);
    Page<Inquiry> findByStatus(InquiryStatus status, Pageable pageable);
    long countByStatus(InquiryStatus status);
}
