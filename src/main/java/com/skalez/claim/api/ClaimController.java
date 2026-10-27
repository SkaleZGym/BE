package com.skalez.claim.api;

import com.skalez.claim.api.ClaimDtos.ClaimResponse;
import com.skalez.claim.api.ClaimDtos.CreateClaimRequest;
import com.skalez.claim.service.ClaimService;
import com.skalez.global.security.LoginMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 고객 교환·반품 신청 */
@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService claimService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClaimResponse request(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateClaimRequest request) {
        return claimService.request(LoginMember.id(jwt), request);
    }

    @GetMapping
    public List<ClaimResponse> getMyClaims(@AuthenticationPrincipal Jwt jwt) {
        return claimService.getMyClaims(LoginMember.id(jwt));
    }
}
