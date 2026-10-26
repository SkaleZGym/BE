package com.skalez.review.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "reviews")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long productId;
    @Column(nullable = false)
    private Long memberId;
    @Column(nullable = false, length = 30)
    private String orderNumber;
    @Column(nullable = false)
    private int rating;
    @Column(nullable = false, length = 1000)
    private String content;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private ReviewStatus status;
    @Column(length = 1000)
    private String reply;
    private Instant repliedAt;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    public Review(Long productId, Long memberId, String orderNumber, int rating, String content){
        if(rating < 1 || rating > 5){
            throw new IllegalArgumentException("별점은 1~5 사이여야 합니다.");
        }
        this.productId = productId;
        this.memberId = memberId;
        this.orderNumber = orderNumber;
        this.rating = rating;
        this.content = content;
        this.status = ReviewStatus.VISIBLE;
        this.createdAt = Instant.now();
    }
    public void changeStatus(ReviewStatus status){this.status = status;}
    public void reply(String reply){
        boolean empty = reply == null || reply.isBlank();
        this.reply = empty ? null : reply.trim();
        this.repliedAt = empty ? null : Instant.now();
    }
}
