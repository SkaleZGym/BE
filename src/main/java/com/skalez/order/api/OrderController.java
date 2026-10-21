package com.skalez.order.api;

import com.skalez.global.security.LoginMember;
import com.skalez.order.api.OrderDtos.ConfirmPaymentRequest;
import com.skalez.order.api.OrderDtos.OrderResponse;
import com.skalez.order.api.OrderDtos.PlaceOrderRequest;
import com.skalez.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse placeOrder(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PlaceOrderRequest request, Locale locale){return orderService.placeOrder(LoginMember.id(jwt), request, locale);}
    @GetMapping
    public List<OrderResponse>getMyOrders(@AuthenticationPrincipal Jwt jwt){return orderService.getMyOrders(LoginMember.id(jwt));}
    @GetMapping("/{orderNumber}")
    public OrderResponse getMyOrder(@AuthenticationPrincipal Jwt jwt, @PathVariable String orderNumber){return orderService.getMyOrder(LoginMember.id(jwt), orderNumber);}
    @PostMapping("/{orderNumber}/payment")
    public OrderResponse confirmPayment(@AuthenticationPrincipal Jwt jwt, @PathVariable String orderNumber, @Valid @RequestBody ConfirmPaymentRequest request){return orderService.confirmPayment(LoginMember.id(jwt), orderNumber, request.paymentId());}
    @PostMapping("/{orderNumber}/cancel")
    public OrderResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable String orderNumber){return orderService.cancel(LoginMember.id(jwt), orderNumber);}
}
