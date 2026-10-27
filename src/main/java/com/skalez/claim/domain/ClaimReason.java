package com.skalez.claim.domain;

/** 신청 사유. 불량·오배송은 회사 부담, 사이즈·단순 변심은 고객 부담 배송비 안내에 쓴다 */
public enum ClaimReason {
    SIZE,
    DEFECT,
    WRONG_ITEM,
    CHANGE_OF_MIND,
    OTHER
}
