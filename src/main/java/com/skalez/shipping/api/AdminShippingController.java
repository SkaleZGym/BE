package com.skalez.shipping.api;

import com.skalez.shipping.api.ShippingDtos.RemoteAreaRequest;
import com.skalez.shipping.api.ShippingDtos.RemoteAreaResponse;
import com.skalez.shipping.api.ShippingDtos.ShippingSettingRequest;
import com.skalez.shipping.api.ShippingDtos.ShippingSettingResponse;
import com.skalez.shipping.service.ShippingAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 관리자 배송비 설정 · 제주/도서산간 우편번호 관리 */
@RestController
@RequestMapping("/api/admin/shipping")
@RequiredArgsConstructor
public class AdminShippingController {
    private final ShippingAdminService shippingAdminService;
    @GetMapping("/settings")
    public List<ShippingSettingResponse> getSettings(){return shippingAdminService.getSettings();}
    @PutMapping("settings/{currency}")
    public ShippingSettingResponse saveSetting(@PathVariable @Pattern(regexp = "[A-Z]{3}") String currency, @Valid @RequestBody ShippingSettingRequest request){return shippingAdminService.saveSetting(currency, request);}
    @GetMapping("/remote-areas")
    public List<RemoteAreaResponse> getRemoteAreas(){return shippingAdminService.getRemoteAreas();}
    @PostMapping("/remote-areas")
    @ResponseStatus(HttpStatus.CREATED)
    public RemoteAreaResponse addRemoteArea(@Valid @RequestBody RemoteAreaRequest request){return shippingAdminService.addRemoteArea(request);}
    @DeleteMapping("/remote-areas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRemoteArea(@PathVariable Long id){shippingAdminService.deleteRemoteArea(id);}
}
