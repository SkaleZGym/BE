package com.skalez.member.api;

import com.skalez.global.security.LoginMember;
import com.skalez.member.api.AuthDtos.LoginRequest;
import com.skalez.member.api.AuthDtos.MemberResponse;
import com.skalez.member.api.AuthDtos.SignupRequest;
import com.skalez.member.api.AuthDtos.TokenResponse;
import com.skalez.member.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

@RestController
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    @PostMapping("/api/auth/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse signup(@Valid @RequestBody SignupRequest request, Locale locale){return authService.signup(request, locale);}
    @PostMapping("/api/auth/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request){return authService.login(request);}
    @GetMapping("/api/members/me")
    public MemberResponse me(@AuthenticationPrincipal Jwt jwt){return authService.me(LoginMember.id(jwt));}
}
