package com.skalez.order.domain;

import com.skalez.global.money.Money;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private Order order;
    @Column(nullable = false)
    private Long productId;
    @Column(nullable = false)
    private Long variantId;
    @Column(nullable = false, length = 200)
    private String productName;
    @Column(nullable = false, length = 10)
    private String size;
    @Column(nullable = false)
    private long unitPrice;
    @Column(nullable = false)
    private int quantity;
    OrderItem(Order order, Long productId, Long variantId, String productName, String size, long unitPrice, int quantity) {
        this.order = order;
        this.productId = productId;
        this.variantId = variantId;
        this.productName = productName;
        this.size = size;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public Money lineTotal() {
        return Money.of(unitPrice, order.getCurrency()).times(quantity);
    }
}
