package com.skalez.global.security.crypto;

import com.skalez.member.domain.Member;
import com.skalez.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class EncryptionTest {

    @Autowired
    AesGcmEncryptor encryptor;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void 같은_값도_매번_다른_암호문이_되고_복호화하면_원래_값() {
        String first = encryptor.encrypt("서울시 강남구 101호");
        String second = encryptor.encrypt("서울시 강남구 101호");

        assertThat(first).isNotEqualTo(second);
        assertThat(encryptor.decrypt(first)).isEqualTo("서울시 강남구 101호");
    }

    @Test
    void 암호문이_조작되면_복호화가_실패한다() {
        String encrypted = encryptor.encrypt("010-1234-5678");
        String tampered = encrypted.substring(0, encrypted.length() - 4) + "AAAA";

        assertThatThrownBy(() -> encryptor.decrypt(tampered)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void DB에는_이름이_암호문으로_비밀번호는_BCrypt로_저장된다() {
        Member saved = memberRepository.save(
                new Member("crypto@example.com", passwordEncoder.encode("pass12345"), "홍길동", "ko"));

        String rawName = jdbcTemplate.queryForObject("select name from members where id = ?", String.class, saved.getId());
        String rawPassword = jdbcTemplate.queryForObject("select password from members where id = ?", String.class, saved.getId());

        assertThat(rawName).isNotEqualTo("홍길동");
        assertThat(encryptor.decrypt(rawName)).isEqualTo("홍길동");
        assertThat(memberRepository.findById(saved.getId()).orElseThrow().getName()).isEqualTo("홍길동");
        assertThat(rawPassword).startsWith("{bcrypt}");
        assertThat(passwordEncoder.matches("pass12345", rawPassword)).isTrue();
    }
}
