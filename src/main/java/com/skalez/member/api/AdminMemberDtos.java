package com.skalez.member.api;

import com.skalez.member.domain.Member;
import com.skalez.member.domain.MemberStatus;
import com.skalez.member.domain.Role;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class AdminMemberDtos {
    private AdminMemberDtos(){}
    public record ChangeStatusRequest(@NotNull MemberStatus status){}
    public record ChangeRoleRequest(@NotNull Role role){}

    /** 아임웹 회원 한 명. joinedAt·marketingConsentAt 은 ISO 시각 (예: 2024-03-01T00:00:00Z) */
    public record ImportMemberRow(String email, String name, String phone, Instant joinedAt, Boolean marketingConsent, Instant marketingConsentAt, String legacyId){}
    /** 한 번에 1,000명까지. 관리자 화면이 나눠서 여러 번 보낸다 */
    public record ImportMembersRequest(@NotEmpty @Size(max = 1000) List<ImportMemberRow> members){}
    /** created: 새로 만든 수, skipped: 이미 있는 이메일, failures: 잘못된 행 (index 는 이번 요청 안의 순번) */
    public record ImportResult(int created, int skipped, List<Failure> failures){
        public record Failure(int index, String email, String reason){}
    }
    /** passwordSet=false: 아임웹에서 옮겨 와 아직 새 비밀번호를 정하지 않은 회원 */
    public record AdminMemberResponse(Long id, String email, String name, String phone, String role, String status, String language,
                                      boolean marketingConsent, String legacySource, boolean passwordSet, Instant createdAt){
        public static AdminMemberResponse from(Member member){
            return new AdminMemberResponse(member.getId(), member.getEmail(), member.getName(), member.getPhone(), member.getRole().name(), member.getStatus().name(), member.getLanguage(),
                    member.isMarketingConsent(), member.getLegacySource(), !member.needsPasswordSetup(), member.getCreatedAt());
        }
    }
}
