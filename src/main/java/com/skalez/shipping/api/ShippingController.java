package com.skalez.shipping.api;

import com.skalez.global.i18n.SupportedLanguage;
import com.skalez.shipping.api.ShippingDtos.CourierResponse;
import com.skalez.shipping.api.ShippingDtos.ShippingPolicyResponse;
import com.skalez.shipping.api.ShippingDtos.ShippingQuoteResponse;
import com.skalez.global.money.Money;
import com.skalez.shipping.domain.Courier;
import com.skalez.shipping.service.ShippingFeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
@RestController
@RequiredArgsConstructor
public class ShippingController {
    private final ShippingFeeService shippingFeeService;
    @GetMapping("/api/shipping/policy")
    public ShippingPolicyResponse getPolicy(Locale locale){
        String currency = SupportedLanguage.from(locale).currency();
        return ShippingPolicyResponse.from(shippingFeeService.settingFor(currency));
    }
    /** 결제 화면: 우편번호를 넣으면 제주·도서산간 추가비까지 넣은 배송비를 미리 보여 준다 (주문 때 서버가 다시 계산하는 값과 같다) */
    @GetMapping("/api/shipping/quote")
    public ShippingQuoteResponse quote(@RequestParam long itemsAmount, @RequestParam(defaultValue = "KR") String countryCode,
                                       @RequestParam(required = false) String postalCode, Locale locale){
        String currency = SupportedLanguage.from(locale).currency();
        Money fee = shippingFeeService.feeFor(Money.of(Math.max(0, itemsAmount), currency), countryCode, postalCode);
        String remoteArea = "KR".equals(countryCode) ? shippingFeeService.remoteAreaOf(postalCode).map(Enum::name).orElse(null) : null;
        return new ShippingQuoteResponse(currency, fee.getAmount(), remoteArea);
    }
    @GetMapping("/api/shipping/couriers")
    public List<CourierResponse> getCouriers(){
        return Arrays.stream(Courier.values()).map(CourierResponse::from).toList();
    }
}
