package com.skalez.order.service;

import com.skalez.order.domain.Order;
import com.skalez.order.domain.OrderStatus;
import com.skalez.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class UnpaidOrderCancelJob {
    static final Duration UNPAID_TIMEOUT = Duration.ofMinutes(30);
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    @Scheduled(fixedDelay = 5 * 60 * 1000, initialDelay = 60 * 1000)
    public int run(){
        Instant before = Instant.now().minus(UNPAID_TIMEOUT);
        int cancelled = 0;
        for (Order order : orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING_PAYMENT, before)){
            try{
                if(orderService.cancelUnpaid(order.getOrderNumber())){
                    cancelled++;
                }
            } catch(RuntimeException e){
                log.warn("결제 대기 주문 자동 취소 실패: {}", order.getOrderNumber(), e);
            }
        }
        if(cancelled > 0){
            log.info("결제 대기 주문 자동 취소: {}건", cancelled);
        }
        return cancelled;
    }
}
