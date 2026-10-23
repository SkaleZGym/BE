package com.skalez.product.domain;

import com.skalez.global.error.BusinessException;
import com.skalez.global.error.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 사이즈별 옵션과 재고. 장바구니·주문은 상품이 아니라 이 옵션 단위로 담는다 */
@Entity
@Table(
        name = "product_variants",
        uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "size"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false, length = 10)
    private String size; // M, L, XL, 2XL ...

    @Column(nullable = false)
    private int stock;

    @Column(nullable = false)
    private int sortOrder;

    /** 두 명이 동시에 마지막 재고를 주문해도 한 명만 성공하도록 (낙관적 락) */
    @Version
    private Long version;

    ProductVariant(Product product, String size, int stock, int sortOrder) {
        this.product = product;
        this.size = size;
        this.stock = stock;
        this.sortOrder = sortOrder;
    }

    public void decreaseStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("수량은 1 이상이어야 합니다: " + quantity);
        }
        if (stock < quantity) {
            throw new BusinessException(ErrorCode.OUT_OF_STOCK);
        }
        stock -= quantity;
    }

    public void increaseStock(int quantity) {
        stock += quantity;
    }

    /** 관리자 재고 수정 */
    public void changeStock(int stock) {
        if (stock < 0) {
            throw new IllegalArgumentException("재고는 0 이상이어야 합니다: " + stock);
        }
        this.stock = stock;
    }
}