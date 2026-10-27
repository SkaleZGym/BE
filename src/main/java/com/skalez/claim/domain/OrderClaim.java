package com.skalez.claim.domain;

import com.skalez.global.error.BusinessException;
import com.skalez.global.error.ErrorCode;
import com.skalez.order.domain.Order;
import com.skalez.shipping.domain.Courier;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** 교환·반품 신청. 주문 하나에 진행 중인 신청은 하나만 둔다 (ClaimService에서 확인) */
@Entity
@Table(name = "order_claims")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private ClaimType type;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private ClaimReason reason;

    @Column(length = 1000)
    private String detail;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private ClaimStatus status;

    @Column(length = 1000)
    private String adminMemo;

    private Long refundAmount;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 20)
    private Courier courier;

    @Column(length = 50)
    private String trackingNumber;

    @ElementCollection
    @CollectionTable(name = "order_claim_items", joinColumns = @JoinColumn(name = "claim_id"))
    private List<ClaimItem> items = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant processedAt;

    private Instant completedAt;

    public OrderClaim(Order order, Long memberId, ClaimType type, ClaimReason reason, String detail, List<ClaimItem> items) {
        this.order = order;
        this.memberId = memberId;
        this.type = type;
        this.reason = reason;
        this.detail = detail;
        this.items.addAll(items);
        this.status = ClaimStatus.REQUESTED;
        this.createdAt = Instant.now();
    }

    /** 진행 중(신청·승인)인지 */
    public boolean isOpen() {
        return status == ClaimStatus.REQUESTED || status == ClaimStatus.APPROVED;
    }

    /** 승인: 고객에게 회수 안내 */
    public void approve(String memo) {
        require(status == ClaimStatus.REQUESTED);
        this.status = ClaimStatus.APPROVED;
        this.adminMemo = memo;
        this.processedAt = Instant.now();
    }

    public void reject(String memo) {
        require(isOpen());
        this.status = ClaimStatus.REJECTED;
        this.adminMemo = memo;
        this.processedAt = Instant.now();
    }

    /** 반품 완료: 회수한 상품을 확인하고 환불한 금액을 남긴다 */
    public void completeReturn(long refundAmount) {
        require(status == ClaimStatus.APPROVED && type == ClaimType.RETURN);
        this.refundAmount = refundAmount;
        this.status = ClaimStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    /** 교환 완료: 새 상품을 보낸 택배 정보 (직접 전달 등이면 비워도 된다) */
    public void completeExchange(Courier courier, String trackingNumber) {
        require(status == ClaimStatus.APPROVED && type == ClaimType.EXCHANGE);
        this.courier = courier;
        this.trackingNumber = trackingNumber;
        this.status = ClaimStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    public String trackingUrl() {
        return courier == null || trackingNumber == null ? null : courier.trackingUrl(trackingNumber);
    }

    private static void require(boolean allowed) {
        if (!allowed) {
            throw new BusinessException(ErrorCode.INVALID_CLAIM_STATUS);
        }
    }
}
