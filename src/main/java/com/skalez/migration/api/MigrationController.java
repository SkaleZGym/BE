package com.skalez.migration.api;

import com.skalez.global.security.LoginMember;
import com.skalez.migration.api.MigrationDtos.ImportResult;
import com.skalez.migration.api.MigrationDtos.LegacyOrderResponse;
import com.skalez.migration.api.MigrationDtos.OrderImportRequest;
import com.skalez.migration.api.MigrationDtos.ReviewImportRequest;
import com.skalez.migration.service.MigrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 아임웹 주문·후기 가져오기 (관리자) + 회원의 이전 쇼핑몰 주문 보기. 상품은 기존 상품 등록 API로 가져온다 */
@RestController
@RequiredArgsConstructor
public class MigrationController {

    private final MigrationService migrationService;

    @PostMapping("/api/admin/import/orders")
    public ImportResult importOrders(@Valid @RequestBody OrderImportRequest request) {
        return migrationService.importOrders(request.orders());
    }

    @PostMapping("/api/admin/import/reviews")
    public ImportResult importReviews(@Valid @RequestBody ReviewImportRequest request) {
        return migrationService.importReviews(request.reviews());
    }

    @GetMapping("/api/orders/legacy")
    public List<LegacyOrderResponse> myLegacyOrders(@AuthenticationPrincipal Jwt jwt) {
        return migrationService.myLegacyOrders(LoginMember.id(jwt));
    }
}
