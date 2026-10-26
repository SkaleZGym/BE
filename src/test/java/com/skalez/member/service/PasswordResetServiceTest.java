package com.skalez.member.service;

import com.skalez.global.error.BusinessException;
import com.skalez.member.domain.Member;
import com.skalez.member.domain.PasswordResetToken;
import com.skalez.member.repository.MemberRepository;
import com.skalez.member.repository.PasswordResetTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 아임웹 이전 회원이 메일 링크로 첫 비밀번호를 정하는 흐름 */
@SpringBootTest
@Transactional
class PasswordResetServiceTest {

    @Autowired PasswordResetService passwordResetService;
    @Autowired MemberRepository memberRepository;
    @Autowired PasswordResetTokenRepository tokenRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void legacyMemberSetsPasswordWithMailToken() {
        Member member = memberRepository.save(Member.importLegacy(Member.LEGACY_IMWEB, "m1", "legacy.test@skalez.test", "기존회원", null, null, false, null));
        assertThat(member.needsPasswordSetup()).isTrue();
        tokenRepository.save(new PasswordResetToken(member.getId(), PasswordResetService.hash("mail-token"), Instant.now().plusSeconds(600)));

        passwordResetService.confirm("mail-token", "New-pass-123");

        // 링크를 만료시키는 일괄 업데이트가 앞서 바꾼 비밀번호를 지우지 않아야 한다 (DB에서 다시 읽어 확인)
        Member reloaded = memberRepository.findById(member.getId()).orElseThrow();
        assertThat(reloaded.needsPasswordSetup()).isFalse();
        assertThat(passwordEncoder.matches("New-pass-123", reloaded.getPassword())).isTrue();
        // 같은 링크는 다시 쓸 수 없다
        assertThatThrownBy(() -> passwordResetService.confirm("mail-token", "Other-pass-123")).isInstanceOf(BusinessException.class);
    }

    @Test
    void expiredTokenIsRejected() {
        Member member = memberRepository.save(Member.importLegacy(Member.LEGACY_IMWEB, null, "legacy.expired@skalez.test", "기존회원", null, null, false, null));
        tokenRepository.save(new PasswordResetToken(member.getId(), PasswordResetService.hash("old-token"), Instant.now().minusSeconds(1)));

        assertThatThrownBy(() -> passwordResetService.confirm("old-token", "New-pass-123")).isInstanceOf(BusinessException.class);
    }
}
