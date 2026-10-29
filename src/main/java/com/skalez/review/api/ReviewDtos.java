package com.skalez.review.api;

import com.skalez.review.domain.Review;
import com.skalez.review.domain.ReviewStatus;
import jakarta.validation.constraints.*;

import java.time.Instant;

public final class ReviewDtos {
    private ReviewDtos() {}

    public record CreateReviewRequest(@NotNull Long productId, @Min(1) @Max(5) int rating, @NotBlank @Size(max = 1000) String content) {}
    public record ChangeReviewStatusRequest(@NotNull ReviewStatus status) {}
    /** 빈 값이면 답글 삭제 */
    public record ReplyRequest(@Size(max = 1000) String reply) {}

    /** 고객 화면용. 작성자 이름은 가린다 (김**) */
    public record ReviewResponse(Long id, Long productId, String author, int rating, String content, String reply, Instant createdAt, Instant repliedAt) { public static ReviewResponse of(Review review, String memberName) {return new ReviewResponse(review.getId(), review.getProductId(), mask(memberName != null ? memberName : review.getAuthorName()), review.getRating(), review.getContent(), review.getReply(), review.getCreatedAt(), review.getRepliedAt());
        }
    }

    public record ReviewSummaryResponse(long count, double average) {}

    /** 관리자 화면용: 상품명·회원 이메일·주문번호까지 */
    public record AdminReviewResponse(Long id, Long productId, String productName, String productImageUrl, Long memberId, String memberEmail, String orderNumber, int rating, String content, String status, String reply, Instant createdAt, Instant repliedAt) {
        public static AdminReviewResponse of(Review review, String productName, String productImageUrl, String memberEmail) {return new AdminReviewResponse(review.getId(), review.getProductId(), productName, productImageUrl, review.getMemberId(), memberEmail, review.getOrderNumber(), review.getRating(), review.getContent(), review.getStatus().name(), review.getReply(), review.getCreatedAt(), review.getRepliedAt());
        }
    }

    static String mask(String name) {
        if (name == null || name.isBlank()) return "***";
        return name.charAt(0) + "*".repeat(Math.max(2, name.length() - 1));
    }
}