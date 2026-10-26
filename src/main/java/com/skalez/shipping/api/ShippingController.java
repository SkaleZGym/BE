package com.skalez.shipping.api;

import com.skalez.global.i18n.SupportedLanguage;
import com.skalez.shipping.api.ShippingDtos.CourierResponse;
import com.skalez.shipping.api.ShippingDtos.ShippingPolicyResponse;
import com.skalez.shipping.domain.Courier;
import com.skalez.shipping.service.ShippingFeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
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
    @GetMapping("/api/shipping/couriers")
    public List<CourierResponse> getCouriers(){
        return Arrays.stream(Courier.values()).map(CourierResponse::from).toList();
    }
}
