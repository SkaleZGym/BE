package com.skalez.migration.service;

import com.skalez.global.error.BusinessException;
import com.skalez.global.error.ErrorCode;
import com.skalez.member.domain.Member;
import com.skalez.member.repository.MemberRepository;
import com.skalez.migration.api.MigrationDtos.Failure;
import com.skalez.migration.api.MigrationDtos.ImportResult;
import com.skalez.migration.api.MigrationDtos.LegacyOrderResponse;
import com.skalez.migration.api.MigrationDtos.OrderRow;
import com.skalez.migration.api.MigrationDtos.ReviewRow;
import com.skalez.migration.domain.LegacyOrder;
import com.skalez.migration.repository.LegacyOrderRepository;
import com.skalez.product.repository.ProductRepository;
import com.skalez.review.domain.Review;
import com.skalez.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 아임웹 주문·후기 가져오기.
 * - 같은 주문번호·같은 후기 표시(legacyId)는 다시 넣지 않는다 → 같은 파일을 두 번 올려도 안전
 * - 주문은 이메일로 회원과 이어진다 (회원을 나중에 옮겨 와도 마이페이지에 보인다)
 * - 후기는 회원이면 회원 후기로, 아니면 작성자 이름만 남긴다. 한 회원이 같은 상품에 이미 후기를 남겼으면 건너뛴다
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MigrationService {

    private final LegacyOrderRepository legacyOrderRepository;
    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public ImportResult importOrders(List<OrderRow> rows) {
        int created = 0;
        int skipped = 0;
        List<Failure> failures = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < rows.size(); i++) {
            OrderRow row = rows.get(i);
            String number = row.orderNumber().trim();
            if (!seen.add(number) || legacyOrderRepository.existsBySourceAndLegacyOrderNumber(LegacyOrder.IMWEB, number)) {
                skipped++;
                continue;
            }
            legacyOrderRepository.save(new LegacyOrder(LegacyOrder.IMWEB, number, normalizeEmail(row.email()), row.orderedAt(),
                    blankToNull(row.status()), row.items().trim(), row.totalAmount(), row.currency() == null ? "KRW" : row.currency()));
            created++;
        }
        return new ImportResult(created, skipped, failures);
    }

    @Transactional
    public ImportResult importReviews(List<ReviewRow> rows) {
        int created = 0;
        int skipped = 0;
        List<Failure> failures = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < rows.size(); i++) {
            ReviewRow row = rows.get(i);
            String legacyId = row.legacyId().trim();
            if (!seen.add(legacyId) || reviewRepository.existsByLegacySourceAndLegacyId(LegacyOrder.IMWEB, legacyId)) {
                skipped++;
                continue;
            }
            if (!productRepository.existsById(row.productId())) {
                failures.add(new Failure(i, legacyId, "상품을 찾을 수 없어요"));
                continue;
            }
            Long memberId = memberRepository.findByEmail(normalizeEmail(row.email()) == null ? "" : normalizeEmail(row.email()))
                    .map(Member::getId)
                    .orElse(null);
            if (memberId != null && reviewRepository.existsByMemberIdAndProductId(memberId, row.productId())) {
                skipped++;
                continue;
            }
            reviewRepository.save(Review.importLegacy(row.productId(), memberId, blankToNull(row.authorName()), row.rating(),
                    row.content().trim(), row.createdAt() == null ? Instant.now() : row.createdAt(), LegacyOrder.IMWEB, legacyId));
            created++;
        }
        return new ImportResult(created, skipped, failures);
    }

    /** 로그인한 회원의 이전 쇼핑몰 주문 (회원 이메일 기준) */
    public List<LegacyOrderResponse> myLegacyOrders(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        return legacyOrderRepository.findByEmailOrderByOrderedAtDesc(member.getEmail()).stream()
                .map(LegacyOrderResponse::from)
                .toList();
    }

    private static String normalizeEmail(String email) {
        return email == null || email.isBlank() ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
