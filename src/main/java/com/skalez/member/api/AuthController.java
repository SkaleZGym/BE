package com.skalez.member.api;

import com.skalez.global.security.LoginMember;
import com.skalez.member.api.AuthDtos.ChangePasswordRequest;
import com.skalez.member.api.AuthDtos.LoginRequest;
import com.skalez.member.api.AuthDtos.MemberResponse;
import com.skalez.member.api.AuthDtos.SignupRequest;
import com.skalez.member.api.AuthDtos.TokenResponse;
import com.skalez.member.api.AuthDtos.UpdateMeRequest;
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
    /** 마이페이지: 이름·기본 언어 수정 */
    @PatchMapping("/api/members/me")
    public MemberResponse updateMe(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateMeRequest request){return authService.updateMe(LoginMember.id(jwt), request);}
    /** 마이페이지: 비밀번호 변경 (현재 비밀번호 확인) */
    @PutMapping("/api/members/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ChangePasswordRequest request){authService.changePassword(LoginMember.id(jwt), request);}

    //관리자
    @PostMapping("/api/admin/auth/login")
    public TokenResponse adminLogin(@Valid @RequestBody LoginRequest request){return authService.adminLogin(request);}
    
}
