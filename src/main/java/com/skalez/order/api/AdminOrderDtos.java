package com.skalez.order.api;

import com.skalez.order.api.OrderDtos.OrderItemResponse;
import com.skalez.order.domain.Order;
import com.skalez.order.domain.OrderStatus;
import com.skalez.order.domain.ShippingAddress;
import com.skalez.shipping.domain.Courier;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class AdminOrderDtos {

    private AdminOrderDtos() {
    }

    /** 배송 중으로 바꿀 때는 courier(CJ, LOTTE, HANJIN, EPOST, LOGEN)와 trackingNumber 필수 */
    public record ChangeOrderStatusRequest(@NotNull OrderStatus status, Courier courier, @Size(max = 50) String trackingNumber) {
    }

    public record ShippingAddressResponse(String recipient, String phone, String countryCode, String postalCode, String address1, String address2) {
        static ShippingAddressResponse from(ShippingAddress address) {
            return new ShippingAddressResponse(address.getRecipient(), address.getPhone(), address.getCountryCode(),
                    address.getPostalCode(), address.getAddress1(), address.getAddress2());
        }
    }

    public record AdminOrderResponse(
            String orderNumber,
            Long memberId,
            String status,
            String currency,
            long itemsAmount,
            long discountAmount,
            long shippingFee,
            long totalAmount,
            String couponCode,
            String paymentId,
            String courier,
            String trackingNumber,
            String trackingUrl,
            ShippingAddressResponse shippingAddress,
            List<OrderItemResponse> items,
            Instant createdAt,
            Instant paidAt,
            Instant shippedAt,
            Instant deliveredAt
    ) {
        public static AdminOrderResponse from(Order order) {
            return new AdminOrderResponse(
                    order.getOrderNumber(),
                    order.getMemberId(),
                    order.getStatus().name(),
                    order.getCurrency(),
                    order.getItemsAmount(),
                    order.getDiscountAmount(),
                    order.getShippingFee(),
                    order.getTotalAmount(),
                    order.getCouponCode(),
                    order.getPaymentId(),
                    order.getCourier() == null ? null : order.getCourier().name(),
                    order.getTrackingNumber(),
                    order.trackingUrl(),
                    ShippingAddressResponse.from(order.getShippingAddress()),
                    order.getItems().stream().map(OrderItemResponse::from).toList(),
                    order.getCreatedAt(),
                    order.getPaidAt(),
                    order.getShippedAt(),
                    order.getDeliveredAt()
            );
        }
    }
}