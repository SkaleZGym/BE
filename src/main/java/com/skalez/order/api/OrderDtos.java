package com.skalez.order.api;

import com.skalez.order.domain.Order;
import com.skalez.order.domain.OrderItem;
import com.skalez.order.domain.ShippingAddress;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/** 주문 요청·응답 모음. 금액은 최소 단위 정수 (KRW=원, USD=센트) */
public final class OrderDtos {

    private OrderDtos() {
    }

    public record ShippingAddressRequest(
            @NotBlank @Size(max = 50) String recipient,
            @NotBlank @Size(max = 30) String phone,
            @NotBlank @Pattern(regexp = "[A-Z]{2}") String countryCode,
            @NotBlank @Size(max = 20) String postalCode,
            @NotBlank @Size(max = 200) String address1,
            @Size(max = 200) String address2
    ) {
        public ShippingAddress toEntity() {
            return new ShippingAddress(recipient, phone, countryCode, postalCode, address1, address2);
        }
    }

    /** 장바구니에 담긴 상품 전체로 주문한다 */
    public record PlaceOrderRequest(
            @Valid @NotNull ShippingAddressRequest shippingAddress
    ) {
    }

    public record ConfirmPaymentRequest(
            @NotBlank String paymentId
    ) {
    }

    public record OrderItemResponse(
            Long productId,
            Long variantId,
            String productName,
            String size,
            long unitPrice,
            int quantity,
            long lineTotal
    ) {
        static OrderItemResponse from(OrderItem item) {
            return new OrderItemResponse(
                    item.getProductId(),
                    item.getVariantId(),
                    item.getProductName(),
                    item.getSize(),
                    item.getUnitPrice(),
                    item.getQuantity(),
                    item.lineTotal().getAmount()
            );
        }
    }

    public record OrderResponse(
            String orderNumber,
            String status,
            String currency,
            long itemsAmount,
            long shippingFee,
            long totalAmount,
            String trackingNumber,
            List<OrderItemResponse> items,
            Instant createdAt,
            Instant paidAt
    ) {
        public static OrderResponse from(Order order) {
            return new OrderResponse(
                    order.getOrderNumber(),
                    order.getStatus().name(),
                    order.getCurrency(),
                    order.getItemsAmount(),
                    order.getShippingFee(),
                    order.getTotalAmount(),
                    order.getTrackingNumber(),
                    order.getItems().stream().map(OrderItemResponse::from).toList(),
                    order.getCreatedAt(),
                    order.getPaidAt()
            );
        }
    }
}