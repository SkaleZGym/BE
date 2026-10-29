package com.skalez.migration.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** 아임웹 등 이전 쇼핑몰의 주문 (보기 전용). 새 주문(Order)과 따로 둔다 */
@Entity
@Table(name = "legacy_orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LegacyOrder {

    public static final String IMWEB = "IMWEB";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String source;

    @Column(nullable = false, length = 50)
    private String legacyOrderNumber;

    @Column(length = 100)
    private String email;

    private Instant orderedAt;

    @Column(length = 50)
    private String statusText;

    @Column(nullable = false, length = 2000)
    private String itemsText;

    private Long totalAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, updatable = false)
    private Instant importedAt;

    public LegacyOrder(String source, String legacyOrderNumber, String email, Instant orderedAt, String statusText,
                       String itemsText, Long totalAmount, String currency) {
        this.source = source;
        this.legacyOrderNumber = legacyOrderNumber;
        this.email = email;
        this.orderedAt = orderedAt;
        this.statusText = statusText;
        this.itemsText = itemsText;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.importedAt = Instant.now();
    }
}
