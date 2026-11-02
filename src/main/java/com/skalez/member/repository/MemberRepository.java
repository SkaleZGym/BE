package com.skalez.member.repository;

import com.skalez.member.domain.Member;
import com.skalez.member.domain.MemberStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);
    boolean existsByEmail(String email);
    Page<Member> findByEmailContaining(String keyword, Pageable pageable);
    Page<Member> findByEmailContainingAndStatus(String keyword, MemberStatus status, Pageable pageable);
    /** 대시보드: 오늘 가입한 회원 수 */
    long countByCreatedAtGreaterThanEqual(Instant from);

    /**
     * 적립금을 바꿀 때 회원 행을 잠그고 읽는다 (select ... for update).
     * 같은 회원이 두 창에서 동시에 주문해도 하나가 끝날 때까지 다른 하나가 기다려서 잔액을 두 번 쓰지 못한다
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Member m where m.id = :id")
    Optional<Member> findForPointsById(@Param("id") Long id);
}
