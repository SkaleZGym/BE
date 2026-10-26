package com.skalez.member.repository;

import com.skalez.member.domain.MemberSocialAccount;
import com.skalez.member.domain.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SocialAccountRepository extends JpaRepository<MemberSocialAccount, Long> {
    Optional<MemberSocialAccount> findByProviderAndProviderUserId(SocialProvider provider, String providerUserId);
    boolean existsByMemberIdAndProvider(Long memberId, SocialProvider provider);
    List<MemberSocialAccount> findByMemberId(Long memberId);
}
