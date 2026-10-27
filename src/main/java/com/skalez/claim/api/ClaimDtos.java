package com.skalez.claim.api;

import com.skalez.claim.domain.ClaimItem;
import com.skalez.claim.domain.ClaimReason;
import com.skalez.claim.domain.ClaimType;
import com.skalez.claim.domain.OrderClaim;
import com.skalez.order.domain.Order;
import com.skalez.order.domain.OrderItem;
import com.skalez.shipping.domain.Courier;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/** 교환·반품 요청·응답. 금액은 주문 통화의 최소 단위 정수 */
public final class ClaimDtos {

    private ClaimDtos() {
    }

    public record ClaimItemRequest(
            @NotNull Long orderItemId,
            @Min(1) int quantity,
            @Size(max = 10) String exchangeSize
    ) {
    }

    /** 예: {"orderNumber":"20261027-AB12CD34","type":"EXCHANGE","reason":"SIZE","items":[{"orderItemId":3,"quantity":1,"exchangeSize":"XL"}]} */
    public record CreateClaimRequest(
            @NotBlank String orderNumber,
            @NotNull ClaimType type,
            @NotNull ClaimReason reason,
            @Size(max = 1000) String detail,
            @NotEmpty @Valid List<ClaimItemRequest> items
    ) {
    }

    /** 승인·거절 때 고객에게 보일 안내 (회수 주소, 거절 사유 등) */
    public record MemoRequest(
            @Size(max = 1000) String memo
    ) {
    }

    /** 반품: refundAmount 필수 · 교환: 다시 보낸 택배 정보 (선택) */
    public record CompleteClaimRequest(
            Long refundAmount,
            Courier courier,
            @Size(max = 50) String trackingNumber
    ) {
    }

    public record ClaimItemResponse(
            Long orderItemId,
            String productName,
            String size,
            long unitPrice,
            int quantity,
            String exchangeSize
    ) {
        static ClaimItemResponse from(ClaimItem item, Order order) {
            OrderItem orderItem = order.getItems().stream()
                    .filter(candidate -> candidate.getId().equals(item.getOrderItemId()))
                    .findFirst()
                    .orElseThrow();
            return new ClaimItemResponse(item.getOrderItemId(), orderItem.getProductName(), orderItem.getSize(),
                    orderItem.getUnitPrice(), item.getQuantity(), item.getExchangeSize());
        }
    }

    /**
     * itemsAmount = 신청한 상품 금액 합계 (반품 환불 금액을 정할 때 참고).
     * 할인·배송비 차감은 관리자가 완료할 때 refundAmount로 정한다
     */
    public record ClaimResponse(
            Long id,
            String orderNumber,
            String currency,
            long orderTotal,
            String recipient,
            String type,
            String reason,
            String detail,
            String status,
            String adminMemo,
            List<ClaimItemResponse> items,
            long itemsAmount,
            Long refundAmount,
            String courier,
            String trackingNumber,
            String trackingUrl,
            Instant createdAt,
            Instant processedAt,
            Instant completedAt
    ) {
        public static ClaimResponse from(OrderClaim claim) {
            Order order = claim.getOrder();
            List<ClaimItemResponse> items = claim.getItems().stream()
                    .map(item -> ClaimItemResponse.from(item, order))
                    .toList();
            long itemsAmount = items.stream().mapToLong(item -> item.unitPrice() * item.quantity()).sum();
            return new ClaimResponse(
                    claim.getId(),
                    order.getOrderNumber(),
                    order.getCurrency(),
                    order.getTotalAmount(),
                    order.getShippingAddress().getRecipient(),
                    claim.getType().name(),
                    claim.getReason().name(),
                    claim.getDetail(),
                    claim.getStatus().name(),
                    claim.getAdminMemo(),
                    items,
                    itemsAmount,
                    claim.getRefundAmount(),
                    claim.getCourier() == null ? null : claim.getCourier().name(),
                    claim.getTrackingNumber(),
                    claim.trackingUrl(),
                    claim.getCreatedAt(),
                    claim.getProcessedAt(),
                    claim.getCompletedAt()
            );
        }
    }
}
