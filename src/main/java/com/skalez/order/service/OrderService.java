package com.skalez.order.service;

import com.skalez.cart.domain.CartItem;
import com.skalez.cart.repository.CartItemRepository;
import com.skalez.coupon.service.CouponService;
import com.skalez.global.error.BusinessException;
import com.skalez.global.error.ErrorCode;
import com.skalez.global.i18n.SupportedLanguage;
import com.skalez.global.money.Money;
import com.skalez.order.api.OrderDtos.OrderResponse;
import com.skalez.order.api.OrderDtos.PlaceOrderRequest;
import com.skalez.order.domain.Order;
import com.skalez.order.domain.OrderItem;
import com.skalez.order.domain.OrderStatus;
import com.skalez.order.domain.ShippingAddress;
import com.skalez.order.repository.OrderRepository;
import com.skalez.payment.service.PaymentService;
import com.skalez.product.domain.Product;
import com.skalez.product.domain.ProductVariant;
import com.skalez.product.repository.ProductVariantRepository;
import com.skalez.shipping.service.ShippingFeeService;
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
    private final CouponService couponService;
    private final ShippingFeeService shippingFeeService;
    private final OrderNotifier orderNotifier;

    /**
     * 장바구니 → 주문. 이 시점에 재고를 먼저 잡아 두고(차감), 결제 실패·취소 시 되돌린다.
     * 금액: 상품 합계 - 할인코드 + 배송비(할인 후 금액 기준 무료배송, 제주·도서산간 추가)
     * 결제하지 않고 방치된 주문은 UnpaidOrderCancelJob이 30분 뒤 자동 취소한다
     */
    @Transactional
    public OrderResponse placeOrder(Long memberId, PlaceOrderRequest request, Locale locale) {
        List<CartItem> cartItems = cartItemRepository.findByMemberIdOrderByCreatedAtAsc(memberId);
        if (cartItems.isEmpty()) {
            throw new BusinessException(ErrorCode.CART_EMPTY);
        }

        SupportedLanguage language = SupportedLanguage.from(locale);
        String currency = language.currency();
        ShippingAddress address = request.shippingAddress().toEntity();
        Order order = new Order(memberId, currency, address);

        for (CartItem cartItem : cartItems) {
            ProductVariant variant = cartItem.getVariant();
            Product product = variant.getProduct();
            variant.decreaseStock(cartItem.getQuantity());
            order.addItem(variant, product.translationFor(language).getName(),
                    product.priceIn(currency), cartItem.getQuantity());
        }

        Money items = order.itemsMoney();
        String couponCode = blankToNull(request.couponCode());
        Money discount = couponCode == null
                ? Money.zero(currency)
                : couponService.redeem(memberId, couponCode, items, order.getOrderNumber());
        Money shippingFee = shippingFeeService.feeFor(items.minus(discount), address.getCountryCode(), address.getPostalCode());
        order.applyAmounts(discount, shippingFee, couponCode == null ? null : couponCode.toUpperCase(Locale.ROOT));

        orderRepository.save(order);
        cartItemRepository.deleteAll(cartItems);
        return OrderResponse.from(order);
    }

    /** FE에서 PG 결제를 마친 뒤 호출. 서버가 PG에 직접 조회해서 금액을 검증한다 */
    @Transactional
    public OrderResponse confirmPayment(Long memberId, String orderNumber, String paymentId) {
        Order order = findMyOrder(memberId, orderNumber);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
        }
        paymentService.confirm(paymentId, order.getOrderNumber(), order.totalMoney());
        order.markPaid(paymentId);
        orderNotifier.paid(order);
        return OrderResponse.from(order);
    }

    /**
     * PG가 결제 결과를 우리 서버로 직접 보내는 방식(나이스페이 returnUrl)에서 호출.
     * 로그인 토큰이 없으므로 주문번호로 찾고, 결제창 금액이 서버 주문 금액과 같은지 먼저 확인한다.
     * 같은 결과가 두 번 들어와도(새로고침 등) 이미 결제된 주문이면 그대로 성공 처리한다.
     */
    @Transactional
    public Order confirmPaymentByPg(String orderNumber, String paymentKey, long requestedAmount) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        if (order.getStatus() == OrderStatus.PAID && paymentKey.equals(order.getPaymentId())) {
            return order;
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
        }
        if (order.totalMoney().getAmount() != requestedAmount) {
            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }
        paymentService.confirm(paymentKey, orderNumber, order.totalMoney());
        order.markPaid(paymentKey);
        orderNotifier.paid(order);
        return order;
    }

    @Transactional
    public OrderResponse cancel(Long memberId, String orderNumber) {
        Order order = findMyOrder(memberId, orderNumber);
        cancelOrder(order);
        return OrderResponse.from(order);
    }

    /**
     * 결제 대기로 오래 남은 주문 자동 취소 (잡아 둔 재고·할인코드를 돌려놓는다).
     * 그 사이 결제가 끝났으면(상태가 바뀌었으면) 건드리지 않는다. 주문마다 따로 저장해서 하나가 실패해도 나머지는 처리된다
     */
    @Transactional
    public boolean cancelUnpaid(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            return false;
        }
        cancelOrder(order);
        return true;
    }

    /** 관리자 취소 (품절·고객 요청 등). 규칙은 고객 취소와 같다 */
    @Transactional
    public Order cancelByAdmin(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        cancelOrder(order);
        return order;
    }

    public List<OrderResponse> getMyOrders(Long memberId) {
        return orderRepository.findByMemberIdOrderByCreatedAtDesc(memberId).stream()
                .map(OrderResponse::from)
                .toList();
    }

    public OrderResponse getMyOrder(Long memberId, String orderNumber) {
        return OrderResponse.from(findMyOrder(memberId, orderNumber));
    }

    /** 결제됐던 주문이면 PG 환불 → 재고 복구 → 할인코드 사용 취소 */
    private void cancelOrder(Order order) {
        boolean wasPaid = order.getStatus() == OrderStatus.PAID;
        order.cancel();

        if (wasPaid) {
            paymentService.refund(order.getPaymentId(), order.getOrderNumber(), order.totalMoney(), "주문 취소");
        }
        for (OrderItem item : order.getItems()) {
            variantRepository.findById(item.getVariantId())
                    .ifPresent(variant -> variant.increaseStock(item.getQuantity()));
        }
        if (order.getCouponCode() != null) {
            couponService.release(order.getOrderNumber());
        }
    }

    /** 다른 회원의 주문번호를 넣어도 "없음"으로 보이게 한다 */
    private Order findMyOrder(Long memberId, String orderNumber) {
        return orderRepository.findByOrderNumberAndMemberId(orderNumber, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}