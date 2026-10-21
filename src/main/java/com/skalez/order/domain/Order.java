package com.skalez.order.domain;

import com.skalez.global.error.BusinessException;
import com.skalez.global.error.ErrorCode;
import com.skalez.global.money.Money;
import com.skalez.product.domain.ProductVariant;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "orders") // order는 SQL 예약어라 복수형 테이블명 사용
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    private static final DateTimeFormatter ORDER_DATE = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 고객에게 보여주는 주문번호. 예: 20261020-3F9A1C2B */
    @Column(nullable = false, unique = true, length = 30)
    private String orderNumber;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private long itemsAmount;

    @Column(nullable = false)
    private long shippingFee;

    @Column(nullable = false)
    private long totalAmount;

    @Embedded
    private ShippingAddress shippingAddress;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Column(length = 100)
    private String paymentId; // PG 결제 ID

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant paidAt;

    public Order(Long memberId, String currency, ShippingAddress shippingAddress) {
        Instant now = Instant.now();
        this.orderNumber = ORDER_DATE.format(now) + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        this.memberId = memberId;
        this.currency = currency;
        this.shippingAddress = shippingAddress;
        this.status = OrderStatus.PENDING_PAYMENT;
        this.createdAt = now;
    }

    public void addItem(ProductVariant variant, String productName, Money unitPrice, int quantity) {
        if (!unitPrice.getCurrency().equals(currency)) {
            throw new IllegalArgumentException("주문 통화와 상품 가격 통화가 다릅니다");
        }
        items.add(new OrderItem(this, variant.getProduct().getId(), variant.getId(),
                productName, variant.getSize(), unitPrice.getAmount(), quantity));
    }

    /** 상품을 다 담은 뒤 호출. 상품 합계 + 배송비 = 결제 금액 */
    public void calculateAmounts() {
        Money items = this.items.stream()
                .map(OrderItem::lineTotal)
                .reduce(Money.zero(currency), Money::plus);
        Money fee = ShippingPolicy.feeFor(items);
        this.itemsAmount = items.getAmount();
        this.shippingFee = fee.getAmount();
        this.totalAmount = items.plus(fee).getAmount();
    }

    public Money totalMoney() {
        return Money.of(totalAmount, currency);
    }

    public void markPaid(String paymentId) {
        if (status != OrderStatus.PENDING_PAYMENT) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
        }
        this.status = OrderStatus.PAID;
        this.paymentId = paymentId;
        this.paidAt = Instant.now();
    }

    /** 고객 취소는 결제 대기·결제 완료 상태에서만 가능 (배송 준비가 시작되면 불가) */
    public void cancel() {
        if (status != OrderStatus.PENDING_PAYMENT && status != OrderStatus.PAID) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
        }
        this.status = OrderStatus.CANCELLED;
    }
}