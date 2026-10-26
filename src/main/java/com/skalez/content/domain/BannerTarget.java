package com.skalez.content.domain;

/** 배너(특히 팝업)를 어느 페이지에서 보여 줄지. FE가 이 값을 보고 띄운다 */
public enum BannerTarget {
    ALL_PAGES, // 모든 페이지
    MAIN_ONLY  // 메인 페이지에서만
}
