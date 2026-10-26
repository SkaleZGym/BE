package com.skalez.shipping.service;

import com.skalez.shipping.domain.RemoteArea;
import com.skalez.shipping.domain.RemoteAreaType;
import com.skalez.shipping.domain.ShippingSetting;
import com.skalez.shipping.repository.RemoteAreaRepository;
import com.skalez.shipping.repository.ShippingSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ShippingDataInitializer implements ApplicationRunner {

    private final ShippingSettingRepository settingRepository;
    private final RemoteAreaRepository remoteAreaRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (settingRepository.count() == 0) {
            settingRepository.save(new ShippingSetting("KRW", 3_000, 100_000L, 3_000, 5_000, 7));
            settingRepository.save(new ShippingSetting("USD", 15_00, 150_00L, 0, 0, 14)); // 해외(2027) 임시값
        }
        if (remoteAreaRepository.count() == 0) {
            remoteAreaRepository.save(new RemoteArea(RemoteAreaType.JEJU, 63000, 63644, "제주특별자치도"));
            remoteAreaRepository.save(new RemoteArea(RemoteAreaType.ISLAND, 40200, 40240, "경북 울릉군"));
        }
    }
}