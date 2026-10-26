package com.skalez.member.repository;

import com.skalez.member.domain.Member;
import com.skalez.member.domain.MemberStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);
    boolean existsByEmail(String email);
    Page<Member> findByEmailContaining(String keyword, Pageable pageable);
    Page<Member> findByEmailContainingAndStatus(String keyword, MemberStatus status, Pageable pageable);
    /** 대시보드: 오늘 가입한 회원 수 */
    long countByCreatedAtGreaterThanEqual(Instant from);
}
