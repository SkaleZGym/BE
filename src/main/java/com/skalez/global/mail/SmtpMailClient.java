package com.skalez.global.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

/**
 * SMTP 발송. AWS SES는 "SMTP 설정"에서 발급한 사용자 이름·비밀번호와
 * email-smtp.ap-northeast-2.amazonaws.com:587 을 spring.mail.* 환경변수에 넣으면 그대로 쓴다.
 * (SES 말고 다른 메일 서비스로 바꿔도 이 코드는 그대로)
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "skalez.mail.provider", havingValue = "smtp")
public class SmtpMailClient implements MailClient {

    private final JavaMailSender mailSender;
    private final MailProperties properties;

    @Override
    public void send(MailMessage message) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, StandardCharsets.UTF_8.name());
            helper.setFrom(properties.from(), properties.fromName());
            helper.setTo(message.to());
            helper.setSubject(message.subject());
            helper.setText(message.text(), message.html());
            mailSender.send(mime);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new IllegalStateException("메일을 만들지 못했습니다", e);
        }
    }
}
