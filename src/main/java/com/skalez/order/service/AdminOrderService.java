package com.skalez.order.service;

import com.skalez.global.error.BusinessException;
import com.skalez.global.error.ErrorCode;
import com.skalez.global.web.PageResponse;
import com.skalez.order.api.AdminOrderDtos.AdminOrderResponse;
import com.skalez.order.domain.Order;
import com.skalez.order.domain.OrderStatus;
import com.skalez.order.repository.OrderRepository;
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
    public PageResponse<AdminOrderResponse> getOrders(OrderStatus status, Pageable pageable){
        Page<Order> orders = status == null ? orderRepository.findAll(pageable):orderRepository.findByStatus(status, pageable);
        return PageResponse.from(orders.map(AdminOrderResponse::from));
    }
    public AdminOrderResponse getOrder(String orderNumber){return AdminOrderResponse.from(find(orderNumber));}
    @Transactional
    public AdminOrderResponse changeStatus(String orderNumber, OrderStatus status, String trackingNumber){
        Order order = find(orderNumber);
        order.changeStatus(status, trackingNumber);
        return AdminOrderResponse.from(order);
    }
    @Transactional
    public AdminOrderResponse cancel(String orderNumber){return AdminOrderResponse.from(orderService.cancelByAdmin(orderNumber));}
    private Order find(String orderNumber){return orderRepository.findByOrderNumber(orderNumber).orElseThrow(()-> new BusinessException(ErrorCode.ORDER_NOT_FOUND));}
}
