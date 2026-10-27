package com.skalez.order.service;

import com.skalez.global.error.BusinessException;
import com.skalez.global.error.ErrorCode;
import com.skalez.global.web.PageResponse;
import com.skalez.order.api.AdminOrderDtos.AdminOrderResponse;
import com.skalez.order.domain.Order;
import com.skalez.order.domain.OrderStatus;
import com.skalez.order.repository.OrderRepository;
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

    public PageResponse<AdminOrderResponse> getOrders(OrderStatus status, Pageable pageable) {
        Page<Order> orders = status == null ? orderRepository.findAll(pageable) : orderRepository.findByStatus(status, pageable);
        return PageResponse.from(orders.map(AdminOrderResponse::from));
    }

    public AdminOrderResponse getOrder(String orderNumber) {
        return AdminOrderResponse.from(find(orderNumber));
    }

    @Transactional
    public AdminOrderResponse changeStatus(String orderNumber, OrderStatus status, Courier courier, String trackingNumber) {
        Order order = find(orderNumber);
        order.changeStatus(status, courier, trackingNumber);
        if (status == OrderStatus.SHIPPED) {
            orderNotifier.shipped(order);
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