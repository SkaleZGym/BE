package com.skalez.member.api;

import com.skalez.member.domain.Member;
import com.skalez.member.domain.MemberStatus;
import com.skalez.member.domain.Role;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public final class AdminMemberDtos {
    private AdminMemberDtos(){}
    public record ChangeStatusRequest(@NotNull MemberStatus status){}
    public record ChangeRoleRequest(@NotNull Role role){}
    public record AdminMemberResponse(Long id, String email, String name, String role, String status, String language, Instant createdAt){
        public static AdminMemberResponse from(Member member){
            return new AdminMemberResponse(member.getId(), member.getEmail(), member.getName(), member.getRole().name(), member.getStatus().name(), member.getLanguage(), member.getCreatedAt());
        }
    }
}
