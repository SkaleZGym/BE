package com.skalez.point.service;

import com.skalez.global.error.BusinessException;
import com.skalez.global.error.ErrorCode;
import com.skalez.global.web.PageResponse;
import com.skalez.member.domain.Member;
import com.skalez.member.repository.MemberRepository;
import com.skalez.order.domain.Order;
import com.skalez.point.api.PointDtos.MyPointsResponse;
import com.skalez.point.api.PointDtos.PointHistoryResponse;
import com.skalez.point.domain.PointHistory;
import com.skalez.point.domain.PointType;
import com.skalez.point.repository.PointHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 적립금. 잔액은 Member.pointBalance, 늘고 준 기록은 point_histories (기록을 다 더하면 잔액).
 * - 적립: 배송 완료 때 실제로 낸 상품 금액(배송비·할인·적립금 사용분 제외)의 1%
 * - 사용: 원화 주문만, 1,000원 이상, 쓰고 나서도 최소 1,000원은 결제 (PG 최소 결제액)
 * - 주문 취소: 쓴 적립금 돌려줌 / 반품 환불: 준 적립금 중 환불한 비율만큼 회수
 * 잔액을 바꿀 때는 회원 행을 잠그고 읽어서, 같은 회원의 동시 주문이 잔액을 두 번 쓰지 못한다
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointService {

    public static final int EARN_RATE_PERCENT = 1;
    public static final long MIN_USE = 1_000;
    public static final long MIN_PAYMENT = 1_000;
    private static final String CURRENCY = "KRW";

    private final MemberRepository memberRepository;
    private final PointHistoryRepository historyRepository;

    /** 주문할 때 적립금 쓰기. payable = 적립금 쓰기 전 결제 금액 (상품 - 할인 + 배송비) */
    @Transactional
    public void use(Long memberId, long points, long payable, String currency, String orderNumber) {
        if (points <= 0) {
            return;
        }
        if (!CURRENCY.equals(currency) || points < MIN_USE || points > payable - MIN_PAYMENT) {
            throw new BusinessException(ErrorCode.POINT_NOT_USABLE);
        }
        Member member = lockedMember(memberId);
        member.changePoints(-points); // 잔액이 모자라면 POINT_NOT_ENOUGH
        record(member, PointType.USE, -points, orderNumber, null);
    }

    /** 주문 취소: 쓴 적립금 돌려주기 */
    @Transactional
    public void restoreUsed(Long memberId, long points, String orderNumber) {
        if (points <= 0) {
            return;
        }
        Member member = lockedMember(memberId);
        member.changePoints(points);
        record(member, PointType.USE_CANCEL, points, orderNumber, "주문 취소");
    }

    /** 배송 완료: 적립. 이미 적립한 주문, 원화가 아닌 주문, 적립할 금액이 없는 주문은 건너뛴다 */
    @Transactional
    public void earnForDelivery(Order order) {
        if (order.getPointsEarned() > 0 || !CURRENCY.equals(order.getCurrency())) {
            return;
        }
        long points = order.pointBaseAmount() * EARN_RATE_PERCENT / 100;
        if (points <= 0) {
            return;
        }
        Member member = lockedMember(order.getMemberId());
        if (member.isWithdrawn()) {
            return; // 탈퇴한 회원에게는 적립하지 않는다
        }
        member.changePoints(points);
        record(member, PointType.EARN, points, order.getOrderNumber(), null);
        order.markPointsEarned(points);
    }

    /** 반품 환불: 준 적립금 × (환불 금액 / 결제 금액) 만큼 회수. 이미 써 버려서 잔액이 모자라면 남은 만큼만 */
    @Transactional
    public void reclaimForReturn(Order order, long refundAmount) {
        long earned = order.getPointsEarned();
        if (earned <= 0 || order.getTotalAmount() <= 0) {
            return;
        }
        long wanted = Math.min(earned, Math.round((double) earned * refundAmount / order.getTotalAmount()));
        Member member = lockedMember(order.getMemberId());
        long taken = Math.min(wanted, member.getPointBalance());
        if (taken > 0) {
            member.changePoints(-taken);
            record(member, PointType.EARN_CANCEL, -taken, order.getOrderNumber(), "반품");
        }
        order.markPointsEarned(earned - wanted); // 같은 반품으로 두 번 회수하지 않게 줄여 둔다
    }

    /** 관리자 지급(+)·차감(-). 사유는 기록에 그대로 남는다 */
    @Transactional
    public MyPointsResponse adjust(Long memberId, long amount, String memo, Pageable pageable) {
        if (amount == 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        Member member = lockedMember(memberId);
        member.changePoints(amount);
        record(member, PointType.ADMIN, amount, null, memo.trim());
        return summary(member, pageable);
    }

    /** 아임웹 적립금 옮기기 (회원 가져오기에서 새로 만든 회원에게만 불린다) */
    @Transactional
    public void migrate(Member member, long points) {
        member.changePoints(points);
        record(member, PointType.MIGRATION, points, null, "아임웹 보유 적립금");
    }

    public MyPointsResponse getPoints(Long memberId, Pageable pageable) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        return summary(member, pageable);
    }

    private MyPointsResponse summary(Member member, Pageable pageable) {
        var histories = historyRepository.findByMemberIdOrderByIdDesc(member.getId(), pageable).map(PointHistoryResponse::from);
        return new MyPointsResponse(member.getPointBalance(), EARN_RATE_PERCENT, MIN_USE, MIN_PAYMENT, PageResponse.from(histories));
    }

    private Member lockedMember(Long memberId) {
        return memberRepository.findForPointsById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private void record(Member member, PointType type, long amount, String orderNumber, String memo) {
        historyRepository.save(new PointHistory(member.getId(), type, amount, member.getPointBalance(), orderNumber, memo));
    }
}