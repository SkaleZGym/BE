package com.skalez.point.api;

import com.skalez.global.security.LoginMember;
import com.skalez.point.api.PointDtos.AdjustPointsRequest;
import com.skalez.point.api.PointDtos.MyPointsResponse;
import com.skalez.point.service.PointService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;

    @GetMapping("/api/members/me/points")
    public MyPointsResponse myPoints(@AuthenticationPrincipal Jwt jwt, @PageableDefault(size = 20) Pageable pageable) {
        return pointService.getPoints(LoginMember.id(jwt), pageable);
    }

    @GetMapping("/api/admin/members/{memberId}/points")
    public MyPointsResponse memberPoints(@PathVariable Long memberId, @PageableDefault(size = 20) Pageable pageable) {
        return pointService.getPoints(memberId, pageable);
    }

    @PostMapping("/api/admin/members/{memberId}/points")
    public MyPointsResponse adjust(@PathVariable Long memberId, @Valid @RequestBody AdjustPointsRequest request) {
        return pointService.adjust(memberId, request.amount(), request.memo(), PageRequest.of(0, 20));
    }
}