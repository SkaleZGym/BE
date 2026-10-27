package com.skalez.claim.service;

import com.skalez.claim.api.ClaimDtos.ClaimItemRequest;
import com.skalez.claim.api.ClaimDtos.ClaimResponse;
import com.skalez.claim.api.ClaimDtos.CompleteClaimRequest;
import com.skalez.claim.api.ClaimDtos.CreateClaimRequest;
import com.skalez.claim.domain.ClaimItem;
import com.skalez.claim.domain.ClaimStatus;
import com.skalez.claim.domain.ClaimType;
import com.skalez.claim.domain.OrderClaim;
import com.skalez.claim.repository.OrderClaimRepository;
import com.skalez.global.error.BusinessException;
import com.skalez.global.error.ErrorCode;
import com.skalez.global.money.Money;
import com.skalez.global.web.PageResponse;
import com.skalez.order.domain.Order;
import com.skalez.order.domain.OrderItem;
import com.skalez.order.domain.OrderStatus;
import com.skalez.order.repository.OrderRepository;
import com.skalez.payment.service.PaymentService;
import com.skalez.product.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 교환·반품.
 * - 고객: 배송 완료 후 7일 안에 신청 (전자상거래법 청약철회 기간), 주문 하나에 진행 중인 신청은 하나
 * - 관리자: 승인(회수 안내) → 상품 확인 후 완료. 반품 완료 때 부분 환불 + 재고 복구
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClaimService {

    static final Duration CLAIM_WINDOW = Duration.ofDays(7);
    private static final Set<ClaimStatus> OPEN = Set.of(ClaimStatus.REQUESTED, ClaimStatus.APPROVED);

    private final OrderClaimRepository claimRepository;
    private final OrderRepository orderRepository;
    private final ProductVariantRepository variantRepository;
    private final PaymentService paymentService;

    @Transactional
    public ClaimResponse request(Long memberId, CreateClaimRequest request) {
        Order order = orderRepository.findByOrderNumberAndMemberId(request.orderNumber(), memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        if (order.getStatus() != OrderStatus.DELIVERED || order.getDeliveredAt() == null
                || order.getDeliveredAt().plus(CLAIM_WINDOW).isBefore(Instant.now())) {
            throw new BusinessException(ErrorCode.CLAIM_NOT_ALLOWED);
        }
        if (claimRepository.existsByOrder_IdAndStatusIn(order.getId(), OPEN)) {
            throw new BusinessException(ErrorCode.CLAIM_ALREADY_OPEN);
        }

        Map<Long, Integer> returned = returnedQuantities(order);
        Set<Long> seen = new HashSet<>();
        List<ClaimItem> items = request.items().stream()
                .map(item -> {
                    if (!seen.add(item.orderItemId())) {
                        throw new BusinessException(ErrorCode.CLAIM_INVALID_ITEMS);
                    }
                    return toItem(order, item, request.type(), returned.getOrDefault(item.orderItemId(), 0));
                })
                .toList();

        OrderClaim claim = new OrderClaim(order, memberId, request.type(), request.reason(), blankToNull(request.detail()), items);
        return ClaimResponse.from(claimRepository.save(claim));
    }

    public List<ClaimResponse> getMyClaims(Long memberId) {
        return claimRepository.findByMemberIdOrderByCreatedAtDesc(memberId).stream()
                .map(ClaimResponse::from)
                .toList();
    }

    public PageResponse<ClaimResponse> getClaims(ClaimStatus status, Pageable pageable) {
        Page<OrderClaim> claims = status == null ? claimRepository.findAll(pageable) : claimRepository.findByStatus(status, pageable);
        return PageResponse.from(claims.map(ClaimResponse::from));
    }

    public ClaimResponse getClaim(Long claimId) {
        return ClaimResponse.from(find(claimId));
    }

    @Transactional
    public ClaimResponse approve(Long claimId, String memo) {
        OrderClaim claim = find(claimId);
        claim.approve(blankToNull(memo));
        return ClaimResponse.from(claim);
    }

    @Transactional
    public ClaimResponse reject(Long claimId, String memo) {
        OrderClaim claim = find(claimId);
        claim.reject(blankToNull(memo));
        return ClaimResponse.from(claim);
    }

    @Transactional
    public ClaimResponse complete(Long claimId, CompleteClaimRequest request) {
        OrderClaim claim = find(claimId);
        if (claim.getType() == ClaimType.RETURN) {
            completeReturn(claim, request.refundAmount());
        } else {
            if (request.courier() != null && blankToNull(request.trackingNumber()) == null) {
                throw new BusinessException(ErrorCode.TRACKING_NUMBER_REQUIRED);
            }
            claim.completeExchange(request.courier(), blankToNull(request.trackingNumber()));
        }
        return ClaimResponse.from(claim);
    }

    /** 환불 금액은 0 ~ 결제 금액. 0원이면 PG를 부르지 않는다 (상품 불량 확인 후 교환으로 돌리는 경우 등) */
    private void completeReturn(OrderClaim claim, Long refundAmount) {
        Order order = claim.getOrder();
        if (refundAmount == null || refundAmount < 0 || refundAmount > order.getTotalAmount()) {
            throw new BusinessException(ErrorCode.CLAIM_REFUND_INVALID);
        }
        claim.completeReturn(refundAmount);
        if (refundAmount > 0) {
            paymentService.refund(order.getPaymentId(), order.getOrderNumber(),
                    Money.of(refundAmount, order.getCurrency()), "반품");
        }
        for (ClaimItem item : claim.getItems()) {
            OrderItem orderItem = findOrderItem(order, item.getOrderItemId());
            variantRepository.findById(orderItem.getVariantId())
                    .ifPresent(variant -> variant.increaseStock(item.getQuantity()));
        }
    }

    /** 주문 상품별로 이미 반품 완료된 수량 */
    private Map<Long, Integer> returnedQuantities(Order order) {
        Map<Long, Integer> returned = new HashMap<>();
        claimRepository.findByOrder_IdAndTypeAndStatus(order.getId(), ClaimType.RETURN, ClaimStatus.COMPLETED)
                .forEach(claim -> claim.getItems().forEach(item -> returned.merge(item.getOrderItemId(), item.getQuantity(), Integer::sum)));
        return returned;
    }

    private static ClaimItem toItem(Order order, ClaimItemRequest request, ClaimType type, int alreadyReturned) {
        OrderItem orderItem = findOrderItem(order, request.orderItemId());
        if (request.quantity() > orderItem.getQuantity() - alreadyReturned) {
            throw new BusinessException(ErrorCode.CLAIM_INVALID_ITEMS);
        }
        String exchangeSize = type == ClaimType.EXCHANGE ? blankToNull(request.exchangeSize()) : null;
        return new ClaimItem(orderItem.getId(), request.quantity(), exchangeSize);
    }

    private static OrderItem findOrderItem(Order order, Long orderItemId) {
        return order.getItems().stream()
                .filter(item -> item.getId().equals(orderItemId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.CLAIM_INVALID_ITEMS));
    }

    private OrderClaim find(Long claimId) {
        return claimRepository.findById(claimId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CLAIM_NOT_FOUND));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
