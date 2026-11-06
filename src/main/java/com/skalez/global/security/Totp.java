package com.skalez.global.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;

/**
 * 관리자 2단계 인증: OTP 앱(Google OTP·Microsoft Authenticator 등)의 6자리 숫자.
 * 국제 표준(RFC 6238) 그대로 — 30초마다 바뀌고, 비밀값은 서버와 휴대폰만 안다
 */
public final class Totp {

    private static final String BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int PERIOD_SECONDS = 30;

    private Totp() {
    }

    /** 새 비밀값: 20바이트를 Base32 32글자로 (OTP 앱에 직접 입력할 때 쓰는 글자) */
    public static String newSecret() {
        byte[] bytes = new byte[20];
        RANDOM.nextBytes(bytes);
        StringBuilder out = new StringBuilder();
        int buffer = 0;
        int bits = 0;
        for (byte b : bytes) {
            buffer = (buffer << 8) | (b & 0xff);
            bits += 8;
            while (bits >= 5) {
                out.append(BASE32.charAt((buffer >> (bits - 5)) & 31));
                bits -= 5;
            }
        }
        return out.toString();
    }

    /**
     * 맞으면 그 코드의 30초 칸 번호, 틀리면 -1.
     * 휴대폰 시계가 조금 어긋나도 되게 바로 앞·뒤 칸(±30초)까지 본다
     */
    public static long matchingStep(String secret, String code, Instant now) {
        if (secret == null || code == null || !code.matches("[0-9]{6}")) {
            return -1;
        }
        long current = now.getEpochSecond() / PERIOD_SECONDS;
        byte[] given = code.getBytes(StandardCharsets.US_ASCII);
        for (long step = current - 1; step <= current + 1; step++) {
            if (MessageDigest.isEqual(code(secret, step).getBytes(StandardCharsets.US_ASCII), given)) {
                return step;
            }
        }
        return -1;
    }

    /** OTP 앱이 QR 로 읽는 주소 (otpauth://totp/SKALEZ:이메일?secret=...) */
    public static String uri(String issuer, String account, String secret) {
        String label = encode(issuer + ":" + account);
        return "otpauth://totp/" + label + "?secret=" + secret + "&issuer=" + encode(issuer) + "&algorithm=SHA1&digits=6&period=" + PERIOD_SECONDS;
    }

    /** 30초 칸 번호(step)의 6자리 코드 */
    static String code(String secret, long step) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(decode(secret), "HmacSHA1"));
            byte[] hash = mac.doFinal(ByteBuffer.allocate(8).putLong(step).array());
            int offset = hash[hash.length - 1] & 0xf;
            int binary = ((hash[offset] & 0x7f) << 24) | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8) | (hash[offset + 3] & 0xff);
            return String.format("%06d", binary % 1_000_000);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] decode(String secret) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int buffer = 0;
        int bits = 0;
        for (char c : secret.toCharArray()) {
            int value = BASE32.indexOf(c);
            if (value < 0) {
                throw new IllegalArgumentException("OTP 비밀값이 Base32 가 아닙니다");
            }
            buffer = (buffer << 5) | value;
            bits += 5;
            if (bits >= 8) {
                out.write((buffer >> (bits - 8)) & 0xff);
                bits -= 8;
            }
        }
        return out.toByteArray();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
