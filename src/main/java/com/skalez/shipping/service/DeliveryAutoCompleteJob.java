package com.skalez.shipping.service;

import com.skalez.order.domain.Order;
import com.skalez.order.domain.OrderStatus;
import com.skalez.order.repository.OrderRepository;
import com.skalez.shipping.repository.ShippingSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryAutoCompleteJob {
    private final OrderRepository orderRepository;
    private final ShippingSettingRepository settingRepository;
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    @Transactional
    public int run(){
        Instant now = Instant.now();
        int done = 0;
        for(var setting : settingRepository.findAll()){
            Instant before = now.minus(Duration.ofDays(setting.getAutoDeliverDays()));
            List<Order> orders = orderRepository.findByStatusAndCurrencyAndShippedAtBefore(OrderStatus.SHIPPED, setting.getCurrency(), before);
            orders.forEach(order -> order.changeStatus(OrderStatus.DELIVERED, null, null));
            done += orders.size();
        }
        if(done > 0){
            log.info("자동 배송 완료 처리: {}건", done);
        }
        return done;
    }
}
