package com.skalez.member.oauth;

import com.skalez.member.domain.SocialProvider;

/** 간편 로그인 제공자 하나 (카카오 · 네이버 · 로컬 시험용) */
public interface OAuthClient {

    SocialProvider provider();

    /** 로그인 창 주소. state 는 FE 가 만든 위조 방지 값 */
    String authorizeUrl(String redirectUri, String state);

    /** 로그인 창이 돌려준 code 로 토큰을 받고, 그 토큰으로 회원 정보를 읽는다 */
    Profile fetchProfile(String code, String redirectUri, String state);

    /** emailVerified: 제공자가 인증을 마친 이메일인지 (같은 이메일의 기존 회원과 이을 때만 쓴다) */
    record Profile(String providerUserId, String email, boolean emailVerified, String name) {
    }
}
