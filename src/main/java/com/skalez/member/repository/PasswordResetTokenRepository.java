package com.skalez.member.repository;

import com.skalez.member.domain.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /** 너무 자주 메일을 보내지 않게 마지막 발급 시각을 본다 */
    Optional<PasswordResetToken> findFirstByMemberIdOrderByCreatedAtDesc(Long memberId);

    /**
     * 비밀번호를 바꾸면 그 회원의 아직 안 쓴 링크를 모두 못 쓰게 한다.
     * flushAutomatically: 먼저 바꾼 비밀번호를 DB에 쓴 뒤 실행 (안 그러면 clear 때 변경이 사라진다)
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update PasswordResetToken t set t.usedAt = :now where t.memberId = :memberId and t.usedAt is null")
    int expireAll(@Param("memberId") Long memberId, @Param("now") Instant now);
}
