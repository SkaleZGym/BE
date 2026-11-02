package com.skalez.point.api;

import com.skalez.global.web.PageResponse;
import com.skalez.point.domain.PointHistory;
import jakarta.validation.constraints.*;

import java.time.Instant;

public final class PointDtos {
    private PointDtos(){}
    public record PointHistoryResponse(Long id, String type, long amount, long balanceAfter, String orderNumber, String memo, Instant createdAt){
        public static PointHistoryResponse from(PointHistory history){
            return new PointHistoryResponse(history.getId(), history.getType().name(), history.getAmount(), history.getBalanceAfter(), history.getOrderNumber(), history.getMemo(), history.getCreatedAt());
        }
    }
    public record MyPointsResponse(long balance, int earnRatePercent, long minUse, long minPayment, PageResponse<PointHistoryResponse> histories){}
    public record AdjustPointsRequest(@NotNull @Min(-10_000_000) @Max(10_000_000) Long amount, @NotBlank @Size(max = 200) String memo){}
}
