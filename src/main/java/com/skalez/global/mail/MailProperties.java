package com.skalez.global.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 메일 발송 설정.
 * provider: log(로컬 — 실제로 보내지 않고 서버 로그에 내용만 남김) | smtp(AWS SES 등 SMTP 서버로 발송)
 * from: 보내는 주소 (예: no-reply@skalez.com — SES에서 도메인 인증이 끝난 주소여야 한다)
 * fromName: 받는 사람에게 보이는 이름 (예: SKALEZ)
 * SMTP 접속 정보는 spring.mail.host/port/username/password 환경변수로만 넣는다 (코드·git에 넣지 않는다)
 */
@ConfigurationProperties("skalez.mail")
public record MailProperties(String provider, String from, String fromName) {
}
