package com.skalez.member.api;

import com.skalez.member.domain.Member;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos(){}
    public record SignupRequest(
            @NotBlank @Email @Size(max = 100) String email,
            @NotBlank @Size(min = 8, max = 64) String password,
            @NotBlank @Size(max = 50) String name,
            /** 이용약관 동의 (필수) */
            @NotNull @AssertTrue Boolean agreeTerms,
            /** 개인정보 수집·이용 동의 (필수). 약관과 따로 받는다 (개인정보 보호법: 동의는 항목별로) */
            @NotNull @AssertTrue Boolean agreePrivacy,
            /** 만 14세 이상 확인 (필수). 14세 미만은 법정대리인 동의가 필요해서 가입을 받지 않는다 */
            @NotNull @AssertTrue Boolean over14,
            /** 마케팅 수신 동의 (선택, 없으면 미동의) */
            Boolean marketingConsent
            ){}
    /** 회원 탈퇴. 비밀번호가 있는 계정은 현재 비밀번호를 한 번 더 확인한다 (간편가입만 한 계정은 비워 보낸다) */
    public record WithdrawRequest(String password){}
    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ){}
    /** 관리자 로그인. code: OTP 앱 6자리 (처음엔 비워 보내고, 응답의 step 을 보고 다시 보낸다) */
    public record AdminLoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password,
            @Pattern(regexp = "[0-9]{6}") String code
    ){}
    /**
     * 관리자 로그인 결과. step: OK(토큰 발급) | CODE(OTP 6자리 입력) | SETUP(처음 — OTP 앱 등록).
     * SETUP 일 때만 otpSecret(앱에 직접 입력용 글자)·otpUri(QR 용 주소)가 온다
     */
    public record AdminLoginResponse(String step, String accessToken, Long expiresIn, String otpSecret, String otpUri){
        public static AdminLoginResponse ok(String accessToken, long expiresIn){return new AdminLoginResponse("OK", accessToken, expiresIn, null, null);}
        public static AdminLoginResponse codeRequired(){return new AdminLoginResponse("CODE", null, null, null, null);}
        public static AdminLoginResponse setup(String secret, String uri){return new AdminLoginResponse("SETUP", null, null, secret, uri);}
    }
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
    /** pointBalance: 적립금 잔액 (결제 화면·마이페이지 표시용) */
    public record MemberResponse(Long id, String email, String name, String role, String language, boolean marketingConsent, long pointBalance){
        public static MemberResponse from(Member member){return new MemberResponse(member.getId(), member.getEmail(), member.getName(), member.getRole().name(),member.getLanguage(), member.isMarketingConsent(), member.getPointBalance());
        }
    }
}
