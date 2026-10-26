package com.skalez.member.domain;

import com.skalez.global.error.BusinessException;
import com.skalez.global.error.ErrorCode;

import java.util.Locale;

/** 간편 로그인 제공자. 주소에는 소문자(kakao, naver)로 쓴다 */
public enum SocialProvider {
    KAKAO,
    NAVER;

    public static SocialProvider fromPath(String value) {
        try {
            return valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    public String path() {
        return name().toLowerCase(Locale.ROOT);
    }
}
