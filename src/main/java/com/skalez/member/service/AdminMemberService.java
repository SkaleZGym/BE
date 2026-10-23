package com.skalez.member.service;

import com.skalez.global.error.BusinessException;
import com.skalez.global.error.ErrorCode;
import com.skalez.global.web.PageResponse;
import com.skalez.member.api.AdminMemberDtos.AdminMemberResponse;
import com.skalez.member.domain.Member;
import com.skalez.member.domain.MemberStatus;
import com.skalez.member.domain.Role;
import com.skalez.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMemberService {

    private final MemberRepository memberRepository;

    public PageResponse<AdminMemberResponse> search(String keyword, MemberStatus status, Pageable pageable) {String email = keyword == null ? "" : keyword.trim();
        Page<Member> members = status == null ? memberRepository.findByEmailContaining(email, pageable) : memberRepository.findByEmailContainingAndStatus(email, status, pageable);return PageResponse.from(members.map(AdminMemberResponse::from));
    }
    public AdminMemberResponse getMember(Long memberId) {return AdminMemberResponse.from(find(memberId));}
    @Transactional
    public AdminMemberResponse changeStatus(Long adminId, Long memberId, MemberStatus status) {
        Member member = findOther(adminId, memberId);
        member.changeStatus(status);
        return AdminMemberResponse.from(member);
    }
    @Transactional
    public AdminMemberResponse changeRole(Long adminId, Long memberId, Role role) {
        Member member = findOther(adminId, memberId);
        member.changeRole(role);
        return AdminMemberResponse.from(member);
    }
    private Member findOther(Long adminId, Long memberId) {if (adminId.equals(memberId)) {throw new BusinessException(ErrorCode.SELF_MODIFICATION);}
        return find(memberId);
    }
    private Member find(Long memberId) {return memberRepository.findById(memberId).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}