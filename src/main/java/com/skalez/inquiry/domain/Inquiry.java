package com.skalez.inquiry.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "inquiries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inquiry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "member_id", nullable =false)
    private Long memberId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InquiryCategory category;
    @Column(nullable = false, length = 100)
    private String title;
    @Column(nullable = false, length = 2000)
    private String content;
    private Long productId;
    @Column(length = 30)
    private String orderNumber;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InquiryStatus status;
    @Column(length = 2000)
    private String answer;
    private Instant answeredAt;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    public Inquiry(Long memberId, InquiryCategory category, String title, String content,Long productId, String orderNumber){
        this.memberId = memberId;
        this.category = category;
        this.title = title;
        this.content = content;
        this.productId = productId;
        this.orderNumber = orderNumber;
        this.status = InquiryStatus.WAITING;
        this.createdAt = Instant.now();
    }
    public void answer(String answer){
        this.answer = answer;
        this.status = InquiryStatus.ANSWERED;
        this.answeredAt = Instant.now();
    }
}
