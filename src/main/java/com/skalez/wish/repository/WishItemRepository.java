package com.skalez.wish.repository;

import com.skalez.wish.domain.WishItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WishItemRepository extends JpaRepository<WishItem, Long> {

    List<WishItem> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    Optional<WishItem> findByMemberIdAndProductId(Long memberId, Long productId);

    boolean existsByMemberIdAndProductId(Long memberId, Long productId);
}