package com.skalez.migration.api;

import com.skalez.migration.domain.LegacyOrder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;

public final class MigrationDtos {
    private MigrationDtos(){}
    public record OrderRow(
            @NotBlank @Size(max = 50) String orderNumber,
            @Size(max = 100) String email,
            Instant orderedAt,
            @Size(max = 50) String status,
            @NotBlank @Size(max = 2000) String items,
            @Min(0) Long totalAmount,
            @Pattern(regexp = "[A-Z]{3}") String currency
    ){}
    public record OrderImportRequest(@NotNull @Size(max = 500) List<@Valid OrderRow> orders){}
    public record ReviewRow(@NotBlank @Size(max = 50) String legacyId, @NotNull Long productId, @Size(max = 100) String email, @Size(max = 50) String authorName, @Min(1)@Max(5) int rating, @NotBlank @Size(max = 1000) String content, Instant createdAt){}
    public record ReviewImportRequest(@NotNull @Size(max = 500) List<@Valid ReviewRow> reviews){}
    public record Failure(int index, String key, String reason){}
    public record ImportResult(int created, int skipped, List<Failure> failures){}
    public record LegacyOrderResponse(String orderNumber, Instant orderedAt, String status, String items, Long totalAmount, String currency){
        public static LegacyOrderResponse from(LegacyOrder order){
            return new LegacyOrderResponse(order.getLegacyOrderNumber(), order.getOrderedAt(), order.getStatusText(), order.getItemsText(), order.getTotalAmount(), order.getCurrency());
        }
    }
}
