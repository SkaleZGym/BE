package com.skalez.claim.api;

import com.skalez.claim.api.ClaimDtos.ClaimResponse;
import com.skalez.claim.api.ClaimDtos.CompleteClaimRequest;
import com.skalez.claim.api.ClaimDtos.MemoRequest;
import com.skalez.claim.domain.ClaimStatus;
import com.skalez.claim.service.ClaimService;
import com.skalez.global.web.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

/** 관리자 교환·반품 처리. 예: GET /api/admin/claims?status=REQUESTED */
@RestController
@RequestMapping("/api/admin/claims")
@RequiredArgsConstructor
public class AdminClaimController {

    private final ClaimService claimService;

    @GetMapping
    public PageResponse<ClaimResponse> getClaims(
            @RequestParam(required = false) ClaimStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return claimService.getClaims(status, pageable);
    }

    @GetMapping("/{claimId}")
    public ClaimResponse getClaim(@PathVariable Long claimId) {
        return claimService.getClaim(claimId);
    }

    @PostMapping("/{claimId}/approve")
    public ClaimResponse approve(@PathVariable Long claimId, @Valid @RequestBody MemoRequest request) {
        return claimService.approve(claimId, request.memo());
    }

    @PostMapping("/{claimId}/reject")
    public ClaimResponse reject(@PathVariable Long claimId, @Valid @RequestBody MemoRequest request) {
        return claimService.reject(claimId, request.memo());
    }

    /** 반품 {"refundAmount":59000} · 교환 {"courier":"CJ","trackingNumber":"1234"} */
    @PostMapping("/{claimId}/complete")
    public ClaimResponse complete(@PathVariable Long claimId, @Valid @RequestBody CompleteClaimRequest request) {
        return claimService.complete(claimId, request);
    }
}
