package com.skalez.member.api;

import com.skalez.global.security.LoginMember;
import com.skalez.member.api.AuthDtos.ChangePasswordRequest;
import com.skalez.member.api.AuthDtos.LoginRequest;
import com.skalez.member.api.AuthDtos.MemberResponse;
import com.skalez.member.api.AuthDtos.PasswordResetConfirmRequest;
import com.skalez.member.api.AuthDtos.PasswordResetRequest;
import com.skalez.member.api.AuthDtos.RefreshRequest;
import com.skalez.member.api.AuthDtos.SignupRequest;
import com.skalez.member.api.AuthDtos.TokenResponse;
import com.skalez.member.api.AuthDtos.UpdateMeRequest;
import com.skalez.member.service.AuthService;
import com.skalez.member.service.PasswordResetService;
import com.skalez.member.service.RefreshTokenService;
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
    private final PasswordResetService passwordResetService;
    private final RefreshTokenService refreshTokenService;
    @PostMapping("/api/auth/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse signup(@Valid @RequestBody SignupRequest request, Locale locale){return authService.signup(request, locale);}
    @PostMapping("/api/auth/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request){return authService.login(request);}
    /** 로그인 유지: 리프레시 토큰으로 새 토큰 한 쌍을 받는다 (쓴 리프레시 토큰은 바로 폐기) */
    @PostMapping("/api/auth/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request){return refreshTokenService.rotate(request.refreshToken());}
    /** 로그아웃: 리프레시 토큰 폐기. 없는 토큰이어도 204 */
    @PostMapping("/api/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshRequest request){refreshTokenService.revoke(request.refreshToken());}
    /** 비밀번호 찾기 메일 요청. 가입 여부와 상관없이 항상 204 */
    @PostMapping("/api/auth/password-reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void requestPasswordReset(@Valid @RequestBody PasswordResetRequest request){passwordResetService.request(request.email());}
    /** 메일 링크의 토큰으로 새 비밀번호 설정 (아임웹 이전 회원의 첫 설정도 같은 길) */
    @PostMapping("/api/auth/password-reset/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request){passwordResetService.confirm(request.token(), request.newPassword());}
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
