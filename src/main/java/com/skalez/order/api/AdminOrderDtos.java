package com.skalez.order.api;

import com.skalez.order.api.OrderDtos.OrderItemResponse;
import com.skalez.order.domain.Order;
import com.skalez.order.domain.OrderStatus;
import com.skalez.order.domain.ShippingAddress;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
public final class AdminOrderDtos {
    private AdminOrderDtos(){}
    public record ChangeOrderStatusRequest(@NotNull OrderStatus status, @Size(max = 50) String trackingNumber){}
    public record ShippingAddressResponse(String recipient, String phone, String countryCode, String postalCode, String address1, String address2){
        static ShippingAddressResponse from(ShippingAddress address){
            return new ShippingAddressResponse(address.getRecipient(), address.getPhone(), address.getCountryCode(), address.getPostalCode(), address.getAddress1(), address.getAddress2());
        }
    }
    public record AdminOrderResponse(String orderNumber, Long memberId, String status, String currency, long itemsAmount, long shippingFee, long totalAmount, String paymentId, String trackingNumber, ShippingAddressResponse shippingAddress, List<OrderItemResponse> items, Instant createdAt, Instant paidAt, Instant shippedAt){
        public static AdminOrderResponse from(Order order){return new AdminOrderResponse(
                order.getOrderNumber(),order.getMemberId(),order.getStatus().name(),order.getCurrency(),order.getItemsAmount(),order.getShippingFee(),order.getTotalAmount(),order.getPaymentId(), order.getTrackingNumber(),ShippingAddressResponse.from(order.getShippingAddress()),order.getItems().stream().map(OrderItemResponse::from).toList(),order.getCreatedAt(),order.getPaidAt(),order.getShippedAt());}
    }
}
