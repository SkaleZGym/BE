package com.skalez.cart.domain;

import com.skalez.product.domain.ProductVariant;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;


@Entity
@Table(name = "cart_items", uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "variant_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "member_id", nullable = false)
    private Long memberId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;
    @Column(nullable = false)
    private int quantity;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    public CartItem(Long memberId, ProductVariant variant, int quantity){
        this.memberId = memberId;
        this.variant = variant;
        this.quantity = quantity;
        this.createdAt = Instant.now();
    }
    public void changeQuantity(int quantity){
        this.quantity = quantity;
    }
}
