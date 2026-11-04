package com.skalez.order.service;

import com.skalez.global.error.BusinessException;
import com.skalez.global.error.ErrorCode;
import com.skalez.global.web.PageResponse;
import com.skalez.order.api.AdminOrderDtos.AdminOrderResponse;
import com.skalez.order.domain.Order;
import com.skalez.order.domain.OrderStatus;
import com.skalez.order.repository.OrderRepository;
import com.skalez.point.service.PointService;
import com.skalez.shipping.domain.Courier;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminOrderService {

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final OrderNotifier orderNotifier;
    private final PointService pointService;

    public PageResponse<AdminOrderResponse> getOrders(OrderStatus status, Pageable pageable) {
        Page<Order> orders = status == null ? orderRepository.findAll(pageable) : orderRepository.findByStatus(status, pageable);
        return PageResponse.from(orders.map(AdminOrderResponse::from));
    }

    public AdminOrderResponse getOrder(String orderNumber) {
        return AdminOrderResponse.from(find(orderNumber));
    }

    @Transactional
    public AdminOrderResponse changeStatus(String orderNumber, OrderStatus status, Courier courier, String trackingNumber) {
        // 고객 취소와 동시에 들어와도 한쪽씩 처리되게 잠근다
        Order order = orderRepository.findForUpdateByOrderNumber(orderNumber)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        order.changeStatus(status, courier, trackingNumber);
        if (status == OrderStatus.SHIPPED) {
            orderNotifier.shipped(order);
        }
        if (status == OrderStatus.DELIVERED) {
            pointService.earnForDelivery(order); // 배송 완료 = 적립
        }
        return AdminOrderResponse.from(order);
    }

    @Transactional
    public AdminOrderResponse cancel(String orderNumber) {
        return AdminOrderResponse.from(orderService.cancelByAdmin(orderNumber));
    }

    private Order find(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }
}