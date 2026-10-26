package com.skalez.dashboard.service;

import com.skalez.dashboard.api.DashboardDtos.*;
import com.skalez.global.i18n.SupportedLanguage;
import com.skalez.inquiry.domain.InquiryStatus;
import com.skalez.inquiry.repository.InquiryRepository;
import com.skalez.member.repository.MemberRepository;
import com.skalez.order.domain.Order;
import com.skalez.order.domain.OrderStatus;
import com.skalez.order.repository.OrderRepository;
import com.skalez.product.domain.ProductStatus;
import com.skalez.product.repository.ProductVariantRepository;
import com.skalez.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {
    /** 매장이 한국에 있어서 '오늘'은 한국 시간으로 자른다 */
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final int DAYS = 7;
    private static final int LOW_STOCK = 3;

    private final OrderRepository orderRepository;
    private final InquiryRepository inquiryRepository;
    private final MemberRepository memberRepository;
    private final ProductVariantRepository variantRepository;
    private final ReviewRepository reviewRepository;

    public DashboardResponse get() {
        LocalDate today = LocalDate.now(SEOUL);
        Instant todayStart = today.atStartOfDay(SEOUL).toInstant();
        Instant weekStart = today.minusDays(DAYS - 1L).atStartOfDay(SEOUL).toInstant();

        // 결제된 주문만 매출로 센다 (취소된 주문은 뺀다). 소규모 매장이라 7일치는 메모리에서 묶어도 충분하다
        List<Order> paid = orderRepository.findByPaidAtGreaterThanEqual(weekStart).stream()
                .filter(order -> order.getStatus() != OrderStatus.CANCELLED)
                .toList();
        Map<LocalDate, List<Order>> byDay = paid.stream()
                .collect(Collectors.groupingBy(order -> LocalDate.ofInstant(order.getPaidAt(), SEOUL)));

        List<SalesDay> salesByDay = new ArrayList<>();
        for (int i = DAYS - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            List<Order> orders = byDay.getOrDefault(date, List.of());
            salesByDay.add(new SalesDay(date, sum(orders, "KRW"), sum(orders, "USD"), orders.size()));
        }
        SalesDay todaySales = salesByDay.get(DAYS - 1);

        List<RecentOrder> recentOrders = orderRepository.findTop5ByOrderByCreatedAtDesc().stream()
                .map(order -> new RecentOrder(order.getOrderNumber(), order.getStatus().name(), order.getCurrency(), order.getTotalAmount(),
                        order.getItems().stream().mapToInt(item -> item.getQuantity()).sum(), order.getCreatedAt()))
                .toList();

        List<LowStock> lowStock = variantRepository.findLowStock(ProductStatus.ON_SALE, LOW_STOCK).stream()
                .map(variant -> new LowStock(variant.getProduct().getId(), variant.getProduct().getSku(),
                        variant.getProduct().translationFor(SupportedLanguage.DEFAULT).getName(), variant.getProduct().getImageUrl(),
                        variant.getId(), variant.getSize(), variant.getStock()))
                .toList();

        return new DashboardResponse(
                new Today(todaySales.orders(), todaySales.salesKrw(), todaySales.salesUsd(), memberRepository.countByCreatedAtGreaterThanEqual(todayStart)),
                salesByDay,
                orderRepository.countByStatusIn(List.of(OrderStatus.PAID, OrderStatus.PREPARING)),
                inquiryRepository.countByStatus(InquiryStatus.WAITING),
                reviewRepository.countByCreatedAtGreaterThanEqual(weekStart),
                memberRepository.count(),
                recentOrders,
                lowStock
        );
    }

    private static long sum(List<Order> orders, String currency) {
        return orders.stream().filter(order -> currency.equals(order.getCurrency())).mapToLong(Order::getTotalAmount).sum();
    }
}