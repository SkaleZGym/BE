package com.skalez.review.service;

import com.skalez.global.error.BusinessException;
import com.skalez.global.error.ErrorCode;
import com.skalez.global.i18n.SupportedLanguage;
import com.skalez.global.web.PageResponse;
import com.skalez.member.domain.Member;
import com.skalez.member.repository.MemberRepository;
import com.skalez.order.domain.Order;
import com.skalez.order.domain.OrderStatus;
import com.skalez.order.repository.OrderRepository;
import com.skalez.product.domain.Product;
import com.skalez.product.repository.ProductRepository;
import com.skalez.review.api.ReviewDtos.AdminReviewResponse;
import com.skalez.review.api.ReviewDtos.CreateReviewRequest;
import com.skalez.review.api.ReviewDtos.ReviewResponse;
import com.skalez.review.api.ReviewDtos.ReviewSummaryResponse;
import com.skalez.review.domain.Review;
import com.skalez.review.domain.ReviewStatus;
import com.skalez.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;

    // 고객
    // || 배송 완료된 주문에 그 상품이 있어야 쓸 수 있음 (상품당 하나)
    @Transactional
    public ReviewResponse create(Long memberId, CreateReviewRequest request){
        Order order= orderRepository.findFirstByMemberIdAndStatusAndItems_ProductIdOrderByDeliveredAtDesc(memberId, OrderStatus.DELIVERED, request.productId())
                .orElseThrow(()-> new BusinessException(ErrorCode.REVIEW_NOT_PURCHASED));
        if(reviewRepository.existsByMemberIdAndProductId(memberId, request.productId())){
            throw new BusinessException(ErrorCode.REVIEW_DUPLICATED);
        }
        Review review = reviewRepository.save(new Review(request.productId(), memberId, order.getOrderNumber(), request.rating(), request.content().trim()));
        return ReviewResponse.of(review, memberNames(List.of(memberId)).get(memberId));
    }
    public PageResponse<ReviewResponse> getProductReviews(Long productId, Pageable pageable){
        Page<Review> reviews = reviewRepository.findByProductIdAndStatus(productId, ReviewStatus.VISIBLE, pageable);
        Map<Long, String> names = memberNames(reviews.map(Review::getMemberId).toList());
        return PageResponse.from(reviews.map(review -> ReviewResponse.of(review,names.get(review.getMemberId()))));
    }
    public ReviewSummaryResponse getSummary(Long productId) {
        Object[] row = reviewRepository.summarize(productId, ReviewStatus.VISIBLE).get(0);
        double average = ((Number) row[1]).doubleValue();
        return new ReviewSummaryResponse(((Number) row[0]).longValue(), Math.round(average * 10) / 10.0);
    }
    public List<ReviewResponse> getMyReviews(Long memberId){
        String name = memberNames(List.of(memberId)).get(memberId);
        return reviewRepository.findByMemberIdOrderByCreatedAtDesc(memberId).stream().map(review -> ReviewResponse.of(review, name)).toList();
    }
    @Transactional
    public void deleteMine(Long memberId, Long reviewId){
        reviewRepository.delete(reviewRepository.findByIdAndMemberId(reviewId, memberId).orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND)));
    }
    public PageResponse<AdminReviewResponse> search(ReviewStatus status, Integer rating, Long productId, Pageable pageable){
        Page<Review> reviews = reviewRepository.search(status, rating, productId, pageable);
        return PageResponse.from(toAdmin(reviews));
    }
    @Transactional
    public AdminReviewResponse changeStatus(Long reviewId, ReviewStatus status){
        Review review = find(reviewId);
        review.changeStatus(status);
        return toAdmin(review);
    }
    @Transactional
    public AdminReviewResponse reply(Long reviewId, String reply){
        Review review = find(reviewId);
        review.reply(reply);
        return toAdmin(review);
    }
    @Transactional
    public void delete(Long reviewId){
        reviewRepository.delete(find(reviewId));
    }
    private Review find(Long reviewId) {
        return reviewRepository.findById(reviewId).orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));
    }
    private AdminReviewResponse toAdmin(Review review) {
        return toAdmin(new PageImpl<>(List.of(review))).getContent().get(0);
    }
    private Page<AdminReviewResponse> toAdmin(Page<Review> reviews){
        Map<Long, Product> products = productRepository.findAllById(reviews.map(Review::getProductId).toSet()).stream().collect(Collectors.toMap(Product::getId, Function.identity()));
        Map<Long, String> emails = memberRepository.findAllById(reviews.map(Review::getMemberId).filter(java.util.Objects::nonNull).toSet()).stream().collect(Collectors.toMap(Member::getId, Member::getEmail));
        return reviews.map(review -> {
            Product product = products.get(review.getProductId());
            String productName = product == null ? null : product.translationFor(SupportedLanguage.DEFAULT).getName();
            return AdminReviewResponse.of(review, productName, product == null ? null : product.getImageUrl(), emails.get(review.getMemberId()));
        });
    }
    /** 이전 쇼핑몰 후기는 회원이 없을 수 있어(null) 빼고 찾는다 */
    private Map<Long, String > memberNames(Collection<Long> memberIds){
        return memberRepository.findAllById(memberIds.stream().filter(java.util.Objects::nonNull).toList()).stream().collect(Collectors.toMap(Member::getId, Member::getName));
    }
}
