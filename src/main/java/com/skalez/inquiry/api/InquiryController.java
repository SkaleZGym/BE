package com.skalez.inquiry.api;

import com.skalez.global.security.LoginMember;
import com.skalez.global.web.PageResponse;
import com.skalez.inquiry.api.InquiryDtos.CreateInquiryRequest;
import com.skalez.inquiry.api.InquiryDtos.InquiryResponse;
import com.skalez.inquiry.service.InquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inquiries")
@RequiredArgsConstructor
public class InquiryController {
    private final InquiryService inquiryService;
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InquiryResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateInquiryRequest request){return inquiryService.create(LoginMember.id(jwt), request);}
    @GetMapping
    public PageResponse<InquiryResponse> getMyInquiries(@AuthenticationPrincipal Jwt jwt, @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)Pageable pageable){return inquiryService.getMyInquiries(LoginMember.id(jwt), pageable);}
    @GetMapping("/{inquiryId}")
    public InquiryResponse getMyInquiry(@AuthenticationPrincipal Jwt jwt, @PathVariable Long inquiryId){return inquiryService.getMyInquiry(LoginMember.id(jwt), inquiryId);}
}
