package com.skalez.inquiry.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
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
    @JdbcTypeCode(SqlTypes.VARCHAR) // enum을 varchar로 저장. H2·PostgreSQL 모두 같은 타입이라 Flyway 스키마 하나로 검증된다
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
    @JdbcTypeCode(SqlTypes.VARCHAR) // enum을 varchar로 저장. H2·PostgreSQL 모두 같은 타입이라 Flyway 스키마 하나로 검증된다
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
    public boolean isAnswered(){
        return status == InquiryStatus.ANSWERED;
    }
    public void answer(String answer){
        this.answer = answer;
        this.status = InquiryStatus.ANSWERED;
        this.answeredAt = Instant.now();
    }
}
