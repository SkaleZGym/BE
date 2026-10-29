package com.skalez.member.repository;

import com.skalez.member.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** 한 회원의 아직 살아 있는 토큰을 모두 폐기 (비밀번호 변경, 토큰 도용 의심) */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update RefreshToken t set t.revokedAt = :now where t.memberId = :memberId and t.revokedAt is null")
    int revokeAll(@Param("memberId") Long memberId, @Param("now") Instant now);

    /** 기간이 지난 토큰 정리 */
    @Modifying
    @Query("delete from RefreshToken t where t.expiresAt < :before")
    int deleteExpired(@Param("before") Instant before);
}
