package com.skalez.order.domain;

import com.skalez.global.error.BusinessException;
import com.skalez.global.error.ErrorCode;
import com.skalez.global.money.Money;
import com.skalez.product.domain.ProductVariant;
import com.skalez.shipping.domain.Courier;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
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
    @JdbcTypeCode(SqlTypes.VARCHAR) // enum을 varchar로 저장. H2·PostgreSQL 모두 같은 타입이라 Flyway 스키마 하나로 검증된다
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private long itemsAmount;

    /** 할인코드로 깎은 금액 */
    @Column(nullable = false)
    private long discountAmount;

    @Column(nullable = false)
    private long shippingFee;

    /** 결제 금액 = 상품 합계 - 할인 + 배송비 */
    @Column(nullable = false)
    private long totalAmount;

    @Column(length = 30)
    private String couponCode;

    @Embedded
    private ShippingAddress shippingAddress;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Column(length = 100)
    private String paymentId; // PG 결제 ID

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR) // enum을 varchar로 저장. H2·PostgreSQL 모두 같은 타입이라 Flyway 스키마 하나로 검증된다
    @Column(length = 20)
    private Courier courier; // 택배사

    @Column(length = 50)
    private String trackingNumber; // 송장번호

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant paidAt;

    private Instant shippedAt;

    private Instant deliveredAt;

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

    /** 담은 상품 합계 */
    public Money itemsMoney() {
        return items.stream()
                .map(OrderItem::lineTotal)
                .reduce(Money.zero(currency), Money::plus);
    }

    /** 상품을 다 담은 뒤 호출. 할인·배송비는 OrderService가 계산해서 넘긴다 */
    public void applyAmounts(Money discount, Money shippingFee, String couponCode) {
        Money items = itemsMoney();
        Money total = items.minus(discount).plus(shippingFee);
        this.itemsAmount = items.getAmount();
        this.discountAmount = discount.getAmount();
        this.shippingFee = shippingFee.getAmount();
        this.totalAmount = total.getAmount();
        this.couponCode = couponCode;
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

    /**
     * 관리자 배송 처리. 순서: 결제 완료 → 상품 준비 중 → 배송 중(택배사·송장번호 필수) → 배송 완료.
     * 상품 준비 중을 건너뛰고 바로 배송 중으로 보내는 것도 허용한다.
     */
    public void changeStatus(OrderStatus next, Courier courier, String trackingNumber) {
        boolean allowed = switch (status) {
            case PAID -> next == OrderStatus.PREPARING || next == OrderStatus.SHIPPED;
            case PREPARING -> next == OrderStatus.SHIPPED;
            case SHIPPED -> next == OrderStatus.DELIVERED;
            default -> false;
        };
        if (!allowed) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
        }
        if (next == OrderStatus.SHIPPED) {
            if (courier == null) {
                throw new BusinessException(ErrorCode.COURIER_REQUIRED);
            }
            if (trackingNumber == null || trackingNumber.isBlank()) {
                throw new BusinessException(ErrorCode.TRACKING_NUMBER_REQUIRED);
            }
            this.courier = courier;
            this.trackingNumber = trackingNumber.trim();
            this.shippedAt = Instant.now();
        }
        if (next == OrderStatus.DELIVERED) {
            this.deliveredAt = Instant.now();
        }
        this.status = next;
    }

    /** 고객 "배송 조회" 버튼 주소. 출고 전이면 null */
    public String trackingUrl() {
        return courier == null || trackingNumber == null ? null : courier.trackingUrl(trackingNumber);
    }

    /** 취소는 결제 대기·결제 완료 상태에서만 가능 (배송 준비가 시작되면 불가) */
    public void cancel() {
        if (status != OrderStatus.PENDING_PAYMENT && status != OrderStatus.PAID) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
        }
        this.status = OrderStatus.CANCELLED;
    }
}