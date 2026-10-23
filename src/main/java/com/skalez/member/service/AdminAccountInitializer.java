package com.skalez.member.service;

import com.skalez.global.i18n.SupportedLanguage;
import com.skalez.global.security.AdminProperties;
import com.skalez.member.domain.Member;
import com.skalez.member.domain.Role;
import com.skalez.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountInitializer implements ApplicationRunner {

    private final AdminProperties properties;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (isBlank(properties.email()) || isBlank(properties.password())) {log.info("관리자 계정 설정(ADMIN_EMAIL / ADMIN_PASSWORD)이 없어 관리자 생성을 건너뜁니다");return;
        }
        String email = properties.email().toLowerCase(Locale.ROOT);
        if (memberRepository.existsByEmail(email)) {return;}
        Member admin = new Member(email, passwordEncoder.encode(properties.password()), isBlank(properties.name()) ? "관리자" : properties.name(), SupportedLanguage.DEFAULT.tag());
        admin.changeRole(Role.ADMIN);
        memberRepository.save(admin);
        log.info("관리자 계정을 만들었습니다: {}", email);
    }
    private boolean isBlank(String value) {return value == null || value.isBlank();}
}