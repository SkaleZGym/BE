package com.skalez.inquiry.api;

import com.skalez.inquiry.domain.Inquiry;
import com.skalez.inquiry.domain.InquiryCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final  class InquiryDtos {
    private InquiryDtos(){}
    public record CreateInquiryRequest(@NotNull InquiryCategory category, @NotBlank @Size(max = 100) String title, @NotBlank @Size(max = 2000) String content, Long productId, @Size(max = 30) String orderNumber){}
    public record AnswerRequest(@NotBlank @Size(max = 2000) String answer){}
    public record InquiryResponse(Long id, Long memberId, String category, String title, String content, Long productId, String orderNumber, String status, String answer, Instant createdAt, Instant answeredAt){
        public static InquiryResponse from(Inquiry inquiry){
            return new InquiryResponse(inquiry.getId(), inquiry.getMemberId(), inquiry.getCategory().name(), inquiry.getTitle(), inquiry.getContent(), inquiry.getProductId(), inquiry.getOrderNumber(), inquiry.getStatus().name(), inquiry.getAnswer(), inquiry.getCreatedAt(), inquiry.getAnsweredAt());
        }
    }
}
