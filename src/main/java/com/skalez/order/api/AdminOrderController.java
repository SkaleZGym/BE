package com.skalez.order.api;

import com.skalez.global.web.PageResponse;
import com.skalez.order.api.AdminOrderDtos.AdminOrderResponse;
import com.skalez.order.api.AdminOrderDtos.ChangeOrderStatusRequest;
import com.skalez.order.domain.OrderStatus;
import com.skalez.order.service.AdminOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    /** 예: GET /api/admin/orders?status=PAID (배송 준비할 주문 목록) */
    @GetMapping
    public PageResponse<AdminOrderResponse> getOrders(
            @RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return adminOrderService.getOrders(status, pageable);
    }

    @GetMapping("/{orderNumber}")
    public AdminOrderResponse getOrder(@PathVariable String orderNumber) {
        return adminOrderService.getOrder(orderNumber);
    }

    /** 예: {"status":"SHIPPED","trackingNumber":"1234567890"} */
    @PatchMapping("/{orderNumber}/status")
    public AdminOrderResponse changeStatus(@PathVariable String orderNumber,
                                           @Valid @RequestBody ChangeOrderStatusRequest request) {
        return adminOrderService.changeStatus(orderNumber, request.status(), request.trackingNumber());
    }

    @PostMapping("/{orderNumber}/cancel")
    public AdminOrderResponse cancel(@PathVariable String orderNumber) {
        return adminOrderService.cancel(orderNumber);
    }
}
