package com.skalez.cart.repository;

import com.skalez.cart.domain.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByMemberIdOrderByCreatedAtAsc(Long memberId);
    Optional<CartItem> findByIdAndMemberId(Long id, Long memberId);
    Optional<CartItem> findByMemberIdAndVariantId(Long memberId, Long variantId);
}
