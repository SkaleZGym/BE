package com.skalez.dashboard.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** 관리자 대시보드. 금액은 최소 단위 (KRW=원, USD=센트), 날짜는 한국 시간 기준 */
public final class DashboardDtos {
    private DashboardDtos() {}

    public record DashboardResponse(
            Today today,
            List<SalesDay> salesByDay,      // 최근 7일 (오래된 날 → 오늘)
            long ordersToShip,              // 결제 완료 + 상품 준비 중
            long waitingInquiries,
            long newReviews,                // 최근 7일
            long memberCount,
            List<RecentOrder> recentOrders, // 최근 5건
            List<LowStock> lowStock         // 판매 중 상품의 재고 3개 이하 사이즈
    ) {}

    public record Today(long orders, long salesKrw, long salesUsd, long newMembers) {}
    public record SalesDay(LocalDate date, long salesKrw, long salesUsd, long orders) {}
    public record RecentOrder(String orderNumber, String status, String currency, long totalAmount, int itemCount, Instant createdAt) {}
    public record LowStock(Long productId, String sku, String productName, String imageUrl, Long variantId, String size, int stock) {}
}