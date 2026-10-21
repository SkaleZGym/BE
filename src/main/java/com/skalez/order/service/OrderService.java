package com.skalez.order.service;

import com.skalez.cart.domain.CartItem;
import com.skalez.cart.repository.CartItemRepository;
import com.skalez.global.error.BusinessException;
import com.skalez.global.error.ErrorCode;
import com.skalez.global.i18n.SupportedLanguage;
import com.skalez.order.api.OrderDtos.OrderResponse;
import com.skalez.order.api.OrderDtos.PlaceOrderRequest;
import com.skalez.order.domain.Order;
import com.skalez.order.domain.OrderItem;
import com.skalez.order.domain.OrderStatus;
import com.skalez.order.repository.OrderRepository;
import com.skalez.payment.service.PaymentService;
import com.skalez.product.domain.Product;
import com.skalez.product.domain.ProductVariant;
import com.skalez.product.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository variantRepository;
    private final PaymentService paymentService;
    @Transactional
    public OrderResponse placeOrder(Long memberId, PlaceOrderRequest request, Locale locale) {
        List<CartItem> cartItems = cartItemRepository.findByMemberIdOrderByCreatedAtAsc(memberId);
        if (cartItems.isEmpty()) {
            throw new BusinessException(ErrorCode.CART_EMPTY);
        }

        SupportedLanguage language = SupportedLanguage.from(locale);
        String currency = language.currency();
        Order order = new Order(memberId, currency, request.shippingAddress().toEntity());

        for (CartItem cartItem : cartItems) {
            ProductVariant variant = cartItem.getVariant();
            Product product = variant.getProduct();
            variant.decreaseStock(cartItem.getQuantity());
            order.addItem(variant, product.translationFor(language).getName(),
                    product.priceIn(currency), cartItem.getQuantity());
        }
        order.calculateAmounts();

        orderRepository.save(order);
        cartItemRepository.deleteAll(cartItems);
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse confirmPayment(Long memberId, String orderNumber, String paymentId) {
        Order order = findMyOrder(memberId, orderNumber);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
        }
        paymentService.confirm(paymentId, order.totalMoney());
        order.markPaid(paymentId);
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse cancel(Long memberId, String orderNumber) {
        Order order = findMyOrder(memberId, orderNumber);
        boolean wasPaid = order.getStatus() == OrderStatus.PAID;
        order.cancel();

        if (wasPaid) {
            paymentService.refund(order.getPaymentId(), order.totalMoney(), "고객 주문 취소");
        }
        for (OrderItem item : order.getItems()) {
            variantRepository.findById(item.getVariantId())
                    .ifPresent(variant -> variant.increaseStock(item.getQuantity()));
        }
        return OrderResponse.from(order);
    }

    public List<OrderResponse> getMyOrders(Long memberId) {
        return orderRepository.findByMemberIdOrderByCreatedAtDesc(memberId).stream()
                .map(OrderResponse::from)
                .toList();
    }

    public OrderResponse getMyOrder(Long memberId, String orderNumber) {
        return OrderResponse.from(findMyOrder(memberId, orderNumber));
    }

    /** 다른 회원의 주문번호를 넣어도 "없음"으로 보이게 한다 */
    private Order findMyOrder(Long memberId, String orderNumber) {
        return orderRepository.findByOrderNumberAndMemberId(orderNumber, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }
}