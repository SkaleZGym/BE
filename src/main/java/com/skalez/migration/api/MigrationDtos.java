package com.skalez.migration.api;

import com.skalez.migration.domain.LegacyOrder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/** 아임웹 데이터 가져오기. 엑셀은 관리자 브라우저에서 읽고, 정리된 행만 보낸다 (한 번에 최대 500건) */
public final class MigrationDtos {

    private MigrationDtos() {
    }

    /** 주문 한 건 (아임웹 엑셀의 같은 주문번호 줄들을 화면에서 묶어 보낸다) */
    public record OrderRow(
            @NotBlank @Size(max = 50) String orderNumber,
            @Size(max = 100) String email,
            Instant orderedAt,
            @Size(max = 50) String status,
            @NotBlank @Size(max = 2000) String items,
            @Min(0) Long totalAmount,
            @Pattern(regexp = "[A-Z]{3}") String currency
    ) {
    }

    public record OrderImportRequest(@NotNull @Size(max = 500) List<@Valid OrderRow> orders) {
    }

    /** 후기 한 건. productId는 화면이 상품명·상품번호로 찾아 넣는다. legacyId는 같은 후기를 두 번 넣지 않기 위한 표시 */
    public record ReviewRow(
            @NotBlank @Size(max = 50) String legacyId,
            @NotNull Long productId,
            @Size(max = 100) String email,
            @Size(max = 50) String authorName,
            @Min(1) @Max(5) int rating,
            @NotBlank @Size(max = 1000) String content,
            Instant createdAt
    ) {
    }

    public record ReviewImportRequest(@NotNull @Size(max = 500) List<@Valid ReviewRow> reviews) {
    }

    public record Failure(int index, String key, String reason) {
    }

    public record ImportResult(int created, int skipped, List<Failure> failures) {
    }

    /** 마이페이지 '이전 쇼핑몰 주문' */
    public record LegacyOrderResponse(String orderNumber, Instant orderedAt, String status, String items, Long totalAmount, String currency) {
        public static LegacyOrderResponse from(LegacyOrder order) {
            return new LegacyOrderResponse(order.getLegacyOrderNumber(), order.getOrderedAt(), order.getStatusText(),
                    order.getItemsText(), order.getTotalAmount(), order.getCurrency());
        }
    }
}
