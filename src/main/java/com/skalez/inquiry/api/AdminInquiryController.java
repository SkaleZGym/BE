package com.skalez.inquiry.api;

import com.skalez.global.web.PageResponse;
import com.skalez.inquiry.api.InquiryDtos.AnswerRequest;
import com.skalez.inquiry.api.InquiryDtos.InquiryResponse;
import com.skalez.inquiry.domain.InquiryStatus;
import com.skalez.inquiry.service.InquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/inquiries")
@RequiredArgsConstructor
public class AdminInquiryController {

    private final InquiryService inquiryService;
    @GetMapping
    public PageResponse<InquiryResponse> getInquiries(@RequestParam(required = false) InquiryStatus status, @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {return inquiryService.getInquiries(status, pageable);}
    @GetMapping("/{inquiryId}")
    public InquiryResponse getInquiry(@PathVariable Long inquiryId) {return inquiryService.getInquiry(inquiryId);}
    @PutMapping("/{inquiryId}/answer")
    public InquiryResponse answer(@PathVariable Long inquiryId, @Valid @RequestBody AnswerRequest request) {return inquiryService.answer(inquiryId, request.answer());}
}