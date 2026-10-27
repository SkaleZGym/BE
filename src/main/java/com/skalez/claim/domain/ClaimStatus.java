package com.skalez.claim.domain;

/** 신청됨 → 승인(상품 회수 중) → 완료. 신청·승인 단계에서 거절할 수 있다 */
public enum ClaimStatus {
    REQUESTED,
    APPROVED,
    REJECTED,
    COMPLETED
}
