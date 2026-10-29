package com.skalez.member.api;

import com.skalez.member.domain.Member;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos(){}
    public record SignupRequest(
            @NotBlank @Email @Size(max = 100) String email,
            @NotBlank @Size(min = 8, max = 64) String password,
            @NotBlank @Size(max = 50) String name,
            /** 이용약관·개인정보 수집 동의 (필수) */
            @NotNull @AssertTrue Boolean agreeTerms,
            /** 마케팅 수신 동의 (선택, 없으면 미동의) */
            Boolean marketingConsent
            ){}
    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ){}
    /** 마이페이지 내 정보 수정. language: ko, en, ja, zh-CN */
    public record UpdateMeRequest(
            @NotBlank @Size(max = 50) String name,
            @NotBlank String language,
            /** null 이면 그대로 둔다 */
            Boolean marketingConsent
    ){}
    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 64) String newPassword
    ){}
    /** 비밀번호 찾기: 가입 여부와 상관없이 항상 204 */
    public record PasswordResetRequest(@NotBlank @Email @Size(max = 100) String email){}
    public record PasswordResetConfirmRequest(
            @NotBlank @Size(max = 100) String token,
            @NotBlank @Size(min = 8, max = 64) String newPassword
    ){}
    /** refreshToken·refreshExpiresIn: 로그인 유지용 (관리자 로그인은 없음 → null) */
    public record TokenResponse(String accessToken, String tokenType, long expiresIn, String refreshToken, Long refreshExpiresIn){}
    public record RefreshRequest(@NotBlank String refreshToken){}
    public record MemberResponse(Long id, String email, String name, String role, String language, boolean marketingConsent){
        public static MemberResponse from(Member member){return new MemberResponse(member.getId(), member.getEmail(), member.getName(), member.getRole().name(),member.getLanguage(), member.isMarketingConsent());
        }
    }
}
