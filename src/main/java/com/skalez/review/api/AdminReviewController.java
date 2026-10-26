package com.skalez.review.api;

import com.skalez.global.web.PageResponse;
import com.skalez.review.api.ReviewDtos.AdminReviewResponse;
import com.skalez.review.api.ReviewDtos.ChangeReviewStatusRequest;
import com.skalez.review.api.ReviewDtos.ReplyRequest;
import com.skalez.review.domain.ReviewStatus;
import com.skalez.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewController {
    private final ReviewService reviewService;
    @GetMapping
    public PageResponse<AdminReviewResponse> getReviews(@RequestParam(required = false) ReviewStatus status, @RequestParam(required = false) Integer rating, @RequestParam(required = false) Long productId, @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {return reviewService.search(status, rating, productId, pageable);}
    @PatchMapping("/{reviewId}/status")
    public AdminReviewResponse changeStatus(@PathVariable Long reviewId, @Valid @RequestBody ChangeReviewStatusRequest request) {return reviewService.changeStatus(reviewId, request.status());}
    @PutMapping("/{reviewId}/reply")
    public AdminReviewResponse reply(@PathVariable Long reviewId, @Valid @RequestBody ReplyRequest request) {return reviewService.reply(reviewId, request.reply());}
    @DeleteMapping("/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long reviewId) {reviewService.delete(reviewId);}
}
