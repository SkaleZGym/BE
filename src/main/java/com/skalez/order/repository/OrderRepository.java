package com.skalez.order.repository;

import com.skalez.order.domain.Order;
import com.skalez.order.domain.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumberAndMemberId(String orderNumber, Long memberId);
    List<Order> findByMemberIdOrderByCreatedAtDesc(Long memberId);
    Optional<Order> findByOrderNumber(String orderNumber);
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
}