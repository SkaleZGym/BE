package com.skalez.member.api;

import com.skalez.global.security.LoginMember;
import com.skalez.global.web.PageResponse;
import com.skalez.member.api.AdminMemberDtos.AdminMemberResponse;
import com.skalez.member.api.AdminMemberDtos.ChangeRoleRequest;
import com.skalez.member.api.AdminMemberDtos.ChangeStatusRequest;
import com.skalez.member.domain.MemberStatus;
import com.skalez.member.service.AdminMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/members")
@RequiredArgsConstructor
public class AdminMemberController {

    private final AdminMemberService adminMemberService;
    @GetMapping
    public PageResponse<AdminMemberResponse> search(@RequestParam(required = false) String keyword, @RequestParam(required = false) MemberStatus status, @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {return adminMemberService.search(keyword, status, pageable);}
    @GetMapping("/{memberId}")
    public AdminMemberResponse getMember(@PathVariable Long memberId) {return adminMemberService.getMember(memberId);}
    @PatchMapping("/{memberId}/status")
    public AdminMemberResponse changeStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable Long memberId, @Valid @RequestBody ChangeStatusRequest request) {return adminMemberService.changeStatus(LoginMember.id(jwt), memberId, request.status());}
    @PatchMapping("/{memberId}/role")
    public AdminMemberResponse changeRole(@AuthenticationPrincipal Jwt jwt, @PathVariable Long memberId, @Valid @RequestBody ChangeRoleRequest request) {return adminMemberService.changeRole(LoginMember.id(jwt), memberId, request.role());}
}