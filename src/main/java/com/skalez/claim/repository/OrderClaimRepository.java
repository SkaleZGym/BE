package com.skalez.claim.repository;

import com.skalez.claim.domain.ClaimStatus;
import com.skalez.claim.domain.ClaimType;
import com.skalez.claim.domain.OrderClaim;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface OrderClaimRepository extends JpaRepository<OrderClaim, Long> {
    /** 이 주문에 진행 중인 신청이 있는지 */
    boolean existsByOrder_IdAndStatusIn(Long orderId, Collection<ClaimStatus> statuses);
    /** 이미 반품 완료된 수량을 빼고 신청하게 */
    List<OrderClaim> findByOrder_IdAndTypeAndStatus(Long orderId, ClaimType type, ClaimStatus status);
    List<OrderClaim> findByMemberIdOrderByCreatedAtDesc(Long memberId);
    Page<OrderClaim> findByStatus(ClaimStatus status, Pageable pageable);
}
