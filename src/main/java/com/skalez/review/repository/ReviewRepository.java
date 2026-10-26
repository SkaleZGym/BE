package com.skalez.review.repository;

import com.skalez.review.domain.Review;
import com.skalez.review.domain.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByProductIdAndStatus(Long productId, ReviewStatus status, Pageable pageable);
    List<Review> findByMemberIdOrderByCreatedAtDesc(Long memberId);
    Optional<Review> findByIdAndMemberId(Long id, Long memberId);
    boolean existsByMemberIdAndProductId(Long memberId, Long productId);
    long countByCreatedAtGreaterThanEqual(Instant from);
    @Query("select count(r), coalesce(avg(r.rating), 0) from Review r where r.productId = :productId and r.status = :status")
    List<Object[]> summarize(@Param("productId")Long productId, @Param("status") ReviewStatus status);
    @Query("""
            select r from Review r
            where (:status is null or r.status = :status)
              and (:rating is null or r.rating = :rating)
              and (:productId is null or r.productId = :productId)
            """)
    Page<Review> search(@Param("status") ReviewStatus status, @Param("rating")Integer rating, @Param("productId") Long productId, Pageable pageable);
}
