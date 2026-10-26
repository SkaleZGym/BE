package com.skalez.product.repository;

import com.skalez.product.domain.ProductStatus;
import com.skalez.product.domain.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    /** 대시보드 품절 임박: 판매 중 상품에서 재고가 max 이하인 사이즈 (적은 순) */
    @Query("select v from ProductVariant v join fetch v.product p where p.status = :status and v.stock <= :max order by v.stock, p.id")
    List<ProductVariant> findLowStock(@Param("status") ProductStatus status, @Param("max") int max);
}
