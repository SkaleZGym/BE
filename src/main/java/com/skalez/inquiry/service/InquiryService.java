package com.skalez.inquiry.service;

import com.skalez.global.error.BusinessException;
import com.skalez.global.error.ErrorCode;
import com.skalez.global.web.PageResponse;
import com.skalez.inquiry.api.InquiryDtos.CreateInquiryRequest;
import com.skalez.inquiry.api.InquiryDtos.InquiryResponse;
import com.skalez.inquiry.domain.Inquiry;
import com.skalez.inquiry.domain.InquiryStatus;
import com.skalez.inquiry.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryService {
    private final InquiryRepository inquiryRepository;
    // -- 고객 -----
    @Transactional
    public InquiryResponse create(Long memberId, CreateInquiryRequest request){Inquiry inquiry = new Inquiry(memberId, request.category(), request.title(), request.content(), request.productId(), request.orderNumber());
    return InquiryResponse.from(inquiryRepository.save(inquiry));
    }
    public PageResponse<InquiryResponse> getMyInquiries(Long memberId, Pageable pageable){return PageResponse.from(inquiryRepository.findByMemberId(memberId, pageable).map(InquiryResponse::from));}
    public InquiryResponse getMyInquiry(Long memberId, Long inquiryId){return inquiryRepository.findByIdAndMemberId(inquiryId, memberId).map(InquiryResponse::from).orElseThrow(() -> new BusinessException(ErrorCode.INQUIRY_NOT_FOUND));}
    // --관리자 -----
    public PageResponse<InquiryResponse> getInquiries(InquiryStatus status, Pageable pageable){
        Page<Inquiry> inquiries = status == null ? inquiryRepository.findAll(pageable):inquiryRepository.findByStatus(status, pageable);
        return PageResponse.from(inquiries.map(InquiryResponse::from));
    }
    public InquiryResponse getInquiry(Long inquiryId){
        return InquiryResponse.from(find(inquiryId));
    }
    @Transactional
    public InquiryResponse answer(Long inquiryId, String answer){
        Inquiry inquiry = find(inquiryId);
        inquiry.answer(answer);
        return InquiryResponse.from(inquiry);
    }
    private Inquiry find(Long inquiryId){return inquiryRepository.findById(inquiryId).orElseThrow(() -> new BusinessException(ErrorCode.INQUIRY_NOT_FOUND));}
}
