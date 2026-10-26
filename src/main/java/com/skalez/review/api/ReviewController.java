package com.skalez.review.api;

import com.skalez.global.security.LoginMember;
import com.skalez.global.web.PageResponse;
import com.skalez.review.api.ReviewDtos.CreateReviewRequest;
import com.skalez.review.api.ReviewDtos.ReviewResponse;
import com.skalez.review.api.ReviewDtos.ReviewSummaryResponse;
import com.skalez.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;
    @GetMapping("/api/products/{productId}/reviews")
    public PageResponse<ReviewResponse> getProductReviews(@PathVariable Long productId, @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable){return reviewService.getProductReviews(productId, pageable);}
    @GetMapping("/api/products/{productId}/reviews/summary")
    public ReviewSummaryResponse getSummary(@PathVariable Long productId){return reviewService.getSummary(productId);}
    @PostMapping("/api/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateReviewRequest request){return reviewService.create(LoginMember.id(jwt), request);}
    @GetMapping("/api/reviews/me")
    public List<ReviewResponse> getMyReviews(@AuthenticationPrincipal Jwt jwt){return reviewService.getMyReviews(LoginMember.id(jwt));}
    @DeleteMapping("/api/reviews/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long reviewId){reviewService.deleteMine(LoginMember.id(jwt), reviewId);}
}
