package com.skalez.global.mail;

/** 메일 한 통 보내기. 구현은 설정(skalez.mail.provider)에 따라 로그용 또는 SMTP */
public interface MailClient {

    void send(MailMessage message);

    /** text: 글자만 보이는 메일 앱용, html: 일반 메일 앱용 */
    record MailMessage(String to, String subject, String text, String html) {
    }
}
