package com.skalez.shipping.service;

import com.skalez.global.error.BusinessException;
import com.skalez.global.error.ErrorCode;
import com.skalez.shipping.api.ShippingDtos.RemoteAreaRequest;
import com.skalez.shipping.api.ShippingDtos.RemoteAreaResponse;
import com.skalez.shipping.api.ShippingDtos.ShippingSettingRequest;
import com.skalez.shipping.api.ShippingDtos.ShippingSettingResponse;
import com.skalez.shipping.domain.RemoteArea;
import com.skalez.shipping.domain.ShippingSetting;
import com.skalez.shipping.repository.RemoteAreaRepository;
import com.skalez.shipping.repository.ShippingSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShippingAdminService {

    private final ShippingSettingRepository settingRepository;
    private final RemoteAreaRepository remoteAreaRepository;

    public List<ShippingSettingResponse> getSettings() {
        return settingRepository.findAll().stream().map(ShippingSettingResponse::from).toList();
    }

    @Transactional
    public ShippingSettingResponse saveSetting(String currency, ShippingSettingRequest request) {
        ShippingSetting setting = settingRepository.findById(currency).orElse(null);
        if (setting == null) {
            setting = settingRepository.save(new ShippingSetting(currency, request.baseFee(), request.freeThreshold(), request.jejuExtraFee(), request.islandExtraFee(), request.autoDeliverDays()));
        } else {setting.change(request.baseFee(), request.freeThreshold(), request.jejuExtraFee(), request.islandExtraFee(), request.autoDeliverDays());}return ShippingSettingResponse.from(setting);
    }

    public List<RemoteAreaResponse> getRemoteAreas() {return remoteAreaRepository.findAllByOrderByZipFromAsc().stream().map(RemoteAreaResponse::from).toList();}
    @Transactional
    public RemoteAreaResponse addRemoteArea(RemoteAreaRequest request) {
        RemoteArea area = new RemoteArea(request.type(), request.zipFrom(), request.zipTo(), request.memo());
        return RemoteAreaResponse.from(remoteAreaRepository.save(area));
    }

    @Transactional
    public void deleteRemoteArea(Long id) {RemoteArea area = remoteAreaRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT));
        remoteAreaRepository.delete(area);
    }
}