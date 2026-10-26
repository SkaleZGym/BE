package com.skalez.global.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 로컬 개발용: 메일을 보내지 않고 서버 로그에 받는 사람·제목·본문을 남긴다.
 * 본문에 비밀번호 재설정 링크가 들어 있으므로 운영에서는 쓰지 않는다 (운영 기본값은 smtp)
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "skalez.mail.provider", havingValue = "log", matchIfMissing = true)
public class LogMailClient implements MailClient {

    @Override
    public void send(MailMessage message) {
        log.info("[메일 · 로컬이라 실제 발송 안 함] to={} subject={}\n{}", message.to(), message.subject(), message.text());
    }
}
