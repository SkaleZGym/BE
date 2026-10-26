package com.skalez.order.repository;

import com.skalez.order.domain.Order;
import com.skalez.order.domain.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumberAndMemberId(String orderNumber, Long memberId);
    List<Order> findByMemberIdOrderByCreatedAtDesc(Long memberId);
    Optional<Order> findByOrderNumber(String orderNumber);
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
    /** 자동 배송 완료 대상: 배송 중이고 출고된 지 오래된 주문 */
    List<Order> findByStatusAndCurrencyAndShippedAtBefore(OrderStatus status, String currency, Instant shippedBefore);
    /** 후기 작성 자격: 그 상품이 들어 있는 배송 완료 주문 */
    Optional<Order> findFirstByMemberIdAndStatusAndItems_ProductIdOrderByDeliveredAtDesc(Long memberId, OrderStatus status, Long productId);
    /** 대시보드 */
    List<Order> findByPaidAtGreaterThanEqual(Instant from);
    List<Order> findTop5ByOrderByCreatedAtDesc();
    long countByStatusIn(Collection<OrderStatus> statuses);
}