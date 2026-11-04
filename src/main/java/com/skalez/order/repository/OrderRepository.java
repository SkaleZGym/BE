package com.skalez.order.repository;

import com.skalez.order.domain.Order;
import com.skalez.order.domain.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumberAndMemberId(String orderNumber, Long memberId);
    List<Order> findByMemberIdOrderByCreatedAtDesc(Long memberId);
    Optional<Order> findByOrderNumber(String orderNumber);
    /**
     * 상태를 바꾸는 곳(결제 승인·취소·관리자 상태 변경)은 이걸로 읽는다.
     * 결제 승인과 취소가 동시에 들어와도 한쪽이 끝날 때까지 기다리게 해서
     * "카드는 결제됐는데 주문은 취소" 나 "취소로 돌려받은 적립금·재고 + 결제 완료" 가 생기지 않게 한다
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Order> findForUpdateByOrderNumber(String orderNumber);
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
    /** 결제 대기로 오래 남은 주문 (자동 취소 대상) */
    List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, Instant createdBefore);
    /** 자동 배송 완료 대상: 배송 중이고 출고된 지 오래된 주문 */
    List<Order> findByStatusAndCurrencyAndShippedAtBefore(OrderStatus status, String currency, Instant shippedBefore);
    /** 후기 작성 자격: 그 상품이 들어 있는 배송 완료 주문 */
    Optional<Order> findFirstByMemberIdAndStatusAndItems_ProductIdOrderByDeliveredAtDesc(Long memberId, OrderStatus status, Long productId);
    /** 대시보드 */
    List<Order> findByPaidAtGreaterThanEqual(Instant from);
    List<Order> findTop5ByOrderByCreatedAtDesc();
    long countByStatusIn(Collection<OrderStatus> statuses);
}