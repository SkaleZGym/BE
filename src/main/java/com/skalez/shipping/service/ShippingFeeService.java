package com.skalez.shipping.service;

import com.skalez.global.error.BusinessException;
import com.skalez.global.error.ErrorCode;
import com.skalez.global.money.Money;
import com.skalez.shipping.domain.RemoteArea;
import com.skalez.shipping.domain.RemoteAreaType;
import com.skalez.shipping.domain.ShippingSetting;
import com.skalez.shipping.repository.RemoteAreaRepository;
import com.skalez.shipping.repository.ShippingSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShippingFeeService {
    private final ShippingSettingRepository settingRepository;
    private final RemoteAreaRepository remoteAreaRepository;
    public Money estimate(Money itemsAmount){
        ShippingSetting setting = settingFor(itemsAmount.getCurrency());
        return Money.of(setting.baseFeeFor(itemsAmount.getAmount()), itemsAmount.getCurrency());
    }
    public Money feeFor(Money itemsAmount, String countryCode, String postalCode){
        ShippingSetting setting = settingFor(itemsAmount.getCurrency());
        long fee = setting.baseFeeFor(itemsAmount.getAmount());
        if("KR".equals(countryCode)&& itemsAmount.getAmount() > 0){
            fee += remoteAreaOf(postalCode).map(setting::extraFeeFor).orElse(0L);
        }
        return Money.of(fee, itemsAmount.getCurrency());
    }
    public Optional<RemoteAreaType> remoteAreaOf(String postalCode){
        Integer zip = parseZip(postalCode);
        if(zip == null){return Optional.empty();}
        return remoteAreaRepository.findByZipFromLessThanEqualAndZipToGreaterThanEqual(zip,zip).stream().map(RemoteArea::getType).max(Comparator.naturalOrder());
    }
    public ShippingSetting settingFor(String currency){return settingRepository.findById(currency).orElseThrow(() -> new BusinessException(ErrorCode.SHIPPING_NOT_AVAILABLE));}
    private static Integer parseZip(String postalCode) {
        if (postalCode == null) {
            return null;
        }
        String digits = postalCode.replaceAll("\\D", "");
        return digits.length() == 5 ? Integer.parseInt(digits) : null;
    }
}
