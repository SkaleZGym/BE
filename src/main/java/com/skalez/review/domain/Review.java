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
    /** 비회원이 남긴 이전 쇼핑몰 후기면 null */
    private Long memberId;
    /** 이전 쇼핑몰 후기면 null */
    @Column(length = 30)
    private String orderNumber;
    /** 이전 쇼핑몰 후기의 작성자 이름 (회원 후기는 회원 이름을 쓴다) */
    @Column(length = 50)
    private String authorName;
    @Column(length = 20)
    private String legacySource;
    @Column(length = 50)
    private String legacyId;
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
    /** 아임웹 등에서 옮겨 온 후기: 작성일을 그대로 두고, 주문 확인 없이 넣는다 */
    public static Review importLegacy(Long productId, Long memberId, String authorName, int rating, String content,
                                      Instant createdAt, String legacySource, String legacyId) {
        Review review = new Review(productId, memberId, null, rating, content);
        review.authorName = authorName;
        review.createdAt = createdAt;
        review.legacySource = legacySource;
        review.legacyId = legacyId;
        return review;
    }
    public void changeStatus(ReviewStatus status){this.status = status;}
    public void reply(String reply){
        boolean empty = reply == null || reply.isBlank();
        this.reply = empty ? null : reply.trim();
        this.repliedAt = empty ? null : Instant.now();
    }
}
