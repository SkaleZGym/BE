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

@RestController
@RequiredArgsConstructor
public class MigrationController {
    private final MigrationService migrationService;
    @PostMapping("/api/admin/import/orders")
    public ImportResult importOrders(@Valid @RequestBody OrderImportRequest request){return migrationService.importOrders(request.orders());}
    @PostMapping("/api/admin/import/reviews")
    public ImportResult importResult(@Valid @RequestBody ReviewImportRequest request){return migrationService.importReviews(request.reviews());}
    @GetMapping("/api/orders/legacy")
    public List<LegacyOrderResponse> myLegacyOrders(@AuthenticationPrincipal Jwt jwt){return migrationService.myLegacyOrders(LoginMember.id(jwt));}
}
