package com.skalez.shipping.repository;

import com.skalez.shipping.domain.ShippingSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShippingSettingRepository extends JpaRepository<ShippingSetting, String> {
}
