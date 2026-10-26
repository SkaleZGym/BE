package com.skalez.member.service;

import com.skalez.global.mail.MailClient.MailMessage;
import org.springframework.web.util.HtmlUtils;

import java.util.Map;

/**
 * 비밀번호 메일 문구 (회원 기본 언어별).
 * setup = 아임웹에서 옮겨 온 회원이 처음 비밀번호를 정하는 메일, 아니면 일반 재설정 메일
 */
final class PasswordMails {
    private PasswordMails() {}

    private record Copy(String subject, String title, String body, String button, String expires, String ignore) {}

    private static final Map<String, Copy> RESET = Map.of(
            "ko", new Copy("[SKALEZ] 비밀번호 재설정 안내", "비밀번호 재설정", "아래 버튼을 눌러 새 비밀번호를 설정해 주세요.", "새 비밀번호 설정하기", "이 링크는 %s 동안만 쓸 수 있어요.", "직접 요청하지 않으셨다면 이 메일은 무시하셔도 돼요. 비밀번호는 바뀌지 않아요."),
            "en", new Copy("[SKALEZ] Reset your password", "Reset your password", "Click the button below to set a new password.", "Set a new password", "This link expires in %s.", "If you didn't request this, you can ignore this email. Your password won't change."),
            "ja", new Copy("[SKALEZ] パスワード再設定のご案内", "パスワード再設定", "下のボタンから新しいパスワードを設定してください。", "新しいパスワードを設定", "このリンクの有効期限は%sです。", "お心当たりがない場合は、このメールを無視してください。パスワードは変更されません。"),
            "zh-CN", new Copy("[SKALEZ] 重置密码", "重置密码", "请点击下方按钮设置新密码。", "设置新密码", "此链接%s内有效。", "如果不是您本人操作，请忽略此邮件，密码不会被更改。")
    );

    private static final Map<String, Copy> SETUP = Map.of(
            "ko", new Copy("[SKALEZ] 새 사이트 비밀번호를 설정해 주세요", "SKALEZ가 새 사이트로 이사했어요", "기존 회원 정보는 그대로 옮겨 왔어요. 보안을 위해 비밀번호만 한 번 새로 설정해 주세요.", "비밀번호 설정하기", "이 링크는 %s 동안만 쓸 수 있어요. 시간이 지나면 '비밀번호 찾기'에서 다시 받을 수 있어요.", "직접 요청하지 않으셨다면 이 메일은 무시하셔도 돼요."),
            "en", new Copy("[SKALEZ] Set your password for our new store", "SKALEZ has moved to a new store", "Your account has been moved over. For security, please set a new password once.", "Set my password", "This link expires in %s. You can request a new one from 'Forgot password'.", "If you didn't request this, you can ignore this email."),
            "ja", new Copy("[SKALEZ] 新しいサイトのパスワードを設定してください", "SKALEZは新しいサイトに移転しました", "会員情報はそのまま引き継がれています。セキュリティのため、パスワードだけ一度新しく設定してください。", "パスワードを設定する", "このリンクの有効期限は%sです。期限が切れた場合は「パスワードをお忘れの方」から再発行できます。", "お心当たりがない場合は、このメールを無視してください。"),
            "zh-CN", new Copy("[SKALEZ] 请设置新网站的密码", "SKALEZ 已迁移到新网站", "您的会员信息已完整迁移。为了安全，请重新设置一次密码。", "设置密码", "此链接%s内有效。过期后可在“忘记密码”中重新获取。", "如果不是您本人操作，请忽略此邮件。")
    );

    static MailMessage build(String to, String language, boolean setup, String link, java.time.Duration ttl) {
        Map<String, Copy> copies = setup ? SETUP : RESET;
        Copy copy = copies.getOrDefault(language, copies.get("ko"));
        String expires = copy.expires().formatted(period(language, ttl));
        String text = copy.title() + "\n\n" + copy.body() + "\n\n" + link + "\n\n" + expires + "\n" + copy.ignore() + "\n\n— SKALEZ";
        String safeLink = HtmlUtils.htmlEscape(link);
        String html = """
                <div style="max-width:480px;margin:0 auto;padding:32px 24px;font-family:-apple-system,'Noto Sans KR',sans-serif;color:#1c1e21">
                  <p style="margin:0 0 24px;font-weight:700;letter-spacing:.2em">SKALEZ</p>
                  <h1 style="margin:0 0 12px;font-size:20px">%s</h1>
                  <p style="margin:0 0 24px;line-height:1.7">%s</p>
                  <p style="margin:0 0 24px"><a href="%s" style="display:inline-block;padding:14px 22px;color:#fff;background:#1c1e21;border-radius:6px;text-decoration:none">%s</a></p>
                  <p style="margin:0 0 6px;font-size:12px;color:#75787d">%s</p>
                  <p style="margin:0 0 24px;font-size:12px;color:#75787d">%s</p>
                  <p style="margin:0;font-size:11px;color:#9a9a9a;word-break:break-all">%s</p>
                </div>
                """.formatted(HtmlUtils.htmlEscape(copy.title()), HtmlUtils.htmlEscape(copy.body()), safeLink, HtmlUtils.htmlEscape(copy.button()),
                HtmlUtils.htmlEscape(expires), HtmlUtils.htmlEscape(copy.ignore()), safeLink);
        return new MailMessage(to, copy.subject(), text, html);
    }

    /** 30분 · 24시간처럼 읽기 쉬운 기간 */
    private static String period(String language, java.time.Duration ttl) {
        long hours = ttl.toHours();
        long minutes = ttl.toMinutes();
        boolean inHours = minutes % 60 == 0;
        return switch (language) {
            case "en" -> inHours ? hours + (hours == 1 ? " hour" : " hours") : minutes + " minutes";
            case "ja" -> inHours ? hours + "時間" : minutes + "分";
            case "zh-CN" -> inHours ? hours + "小时" : minutes + "分钟";
            default -> inHours ? hours + "시간" : minutes + "분";
        };
    }
}
