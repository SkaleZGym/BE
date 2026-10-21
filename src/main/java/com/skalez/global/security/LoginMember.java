package com.skalez.global.security;

import org.springframework.security.oauth2.jwt.Jwt;

public final class LoginMember {
    private LoginMember() {}
    public static Long id(Jwt jwt) {return Long.valueOf(jwt.getSubject());}
}