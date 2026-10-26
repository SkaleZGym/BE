package com.skalez.shipping.api;

import com.skalez.shipping.domain.Courier;
import com.skalez.shipping.domain.RemoteArea;
import com.skalez.shipping.domain.RemoteAreaType;
import com.skalez.shipping.domain.ShippingSetting;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/** 배송 요청·응답. 금액은 최소 단위 (KRW=원, USD=센트) */
public final class ShippingDtos {

    private ShippingDtos() {
    }

    /** 고객 화면 안내용: "3,000원 · 10만원 이상 무료 · 제주 +3,000원" */
    public record ShippingPolicyResponse(String currency, long baseFee, Long freeThreshold, long jejuExtraFee, long islandExtraFee) {
        public static ShippingPolicyResponse from(ShippingSetting setting) {
            return new ShippingPolicyResponse(setting.getCurrency(), setting.getBaseFee(), setting.getFreeThreshold(),
                    setting.getJejuExtraFee(), setting.getIslandExtraFee());
        }
    }

    public record CourierResponse(String code, String name) {
        public static CourierResponse from(Courier courier) {
            return new CourierResponse(courier.name(), courier.displayName());
        }
    }

    public record ShippingSettingRequest(
            @Min(0) long baseFee,
            @Min(0) Long freeThreshold,
            @Min(0) long jejuExtraFee,
            @Min(0) long islandExtraFee,
            @Min(1) @Max(60) int autoDeliverDays
    ) {
    }

    public record ShippingSettingResponse(String currency, long baseFee, Long freeThreshold, long jejuExtraFee,
                                          long islandExtraFee, int autoDeliverDays, Instant updatedAt) {
        public static ShippingSettingResponse from(ShippingSetting setting) {
            return new ShippingSettingResponse(setting.getCurrency(), setting.getBaseFee(), setting.getFreeThreshold(),
                    setting.getJejuExtraFee(), setting.getIslandExtraFee(), setting.getAutoDeliverDays(), setting.getUpdatedAt());
        }
    }

    public record RemoteAreaRequest(
            @NotNull RemoteAreaType type,
            @Min(0) @Max(99999) int zipFrom,
            @Min(0) @Max(99999) int zipTo,
            @Size(max = 100) String memo
    ) {
    }

    public record RemoteAreaResponse(Long id, String type, int zipFrom, int zipTo, String memo) {
        public static RemoteAreaResponse from(RemoteArea area) {
            return new RemoteAreaResponse(area.getId(), area.getType().name(), area.getZipFrom(), area.getZipTo(), area.getMemo());
        }
    }
}