package com.skalez.product.repository;

import com.skalez.category.domain.Category;
import com.skalez.product.domain.Product;
import com.skalez.product.domain.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    /** MEN을 고르면 MEN_TOP, MEN_TIGHTS 같은 하위 카테고리 상품도 함께 나온다 */
    @Query("""
            select p from Product p
            join p.category c
            left join c.parent parent
            where p.status = :status
              and (c.code = :categoryCode or parent.code = :categoryCode)
            """)
    Page<Product> findByCategoryCode(@Param("categoryCode") String categoryCode,
                                     @Param("status") ProductStatus status,
                                     Pageable pageable);

    Optional<Product> findByIdAndStatus(Long id, ProductStatus status);

    boolean existsBySku(String sku);

    /** 숨긴 상품까지 포함해서 그 카테고리를 쓰는 상품이 하나라도 있는지 (카테고리 삭제 가능 여부 판단) */
    boolean existsByCategory(Category category);
}
