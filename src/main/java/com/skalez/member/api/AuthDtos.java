package com.skalez.member.api;

import com.skalez.member.domain.Member;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos(){}
    public record SignupRequest(
            @NotBlank @Email @Size(max = 100) String email,
            @NotBlank @Size(min = 8, max = 64) String password,
            @NotBlank @Size(max = 50) String name
            ){}
    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ){}
    /** 마이페이지 내 정보 수정. language: ko, en, ja, zh-CN */
    public record UpdateMeRequest(
            @NotBlank @Size(max = 50) String name,
            @NotBlank String language
    ){}
    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 64) String newPassword
    ){}
    public record TokenResponse(String accessToken, String tokenType, long expiresIn){}
    public record MemberResponse(Long id, String email, String name, String role, String language){
        public static MemberResponse from(Member member){return new MemberResponse(member.getId(), member.getEmail(), member.getName(), member.getRole().name(),member.getLanguage());
        }
    }
}
