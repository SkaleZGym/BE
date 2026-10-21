package com.skalez.global.security.crypto;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM 양방향 암호화. 이름·전화번호·주소처럼 "다시 읽어야 하는" 개인정보에 쓴다.
 * - 매번 새 IV(12바이트)를 만들어서 같은 값도 암호문이 매번 달라진다.
 * - GCM은 위변조 검사(인증 태그)까지 해 줘서 DB 값이 조작되면 복호화가 실패한다.
 * 저장 형식: Base64( IV 12바이트 + 암호문 + 인증태그 16바이트 )
 */
@Component
public class AesGcmEncryptor {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public AesGcmEncryptor(CryptoProperties properties) {
        String aesKey = properties.aesKey();
        if (aesKey == null || aesKey.isBlank() || aesKey.startsWith("${")) {
            throw new IllegalStateException("AES 키가 없습니다. 환경변수 AES_KEY를 설정해 주세요");
        }
        byte[] keyBytes = Base64.getDecoder().decode(aesKey);
        if (keyBytes.length != 32) {
            throw new IllegalStateException("AES 키는 32바이트(256비트)여야 합니다. 현재: " + keyBytes.length + "바이트");
        }
        this.key = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] stored = ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array();
            return Base64.getEncoder().encodeToString(stored);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("암호화에 실패했습니다", e);
        }
    }

    public String decrypt(String storedText) {
        if (storedText == null) {
            return null;
        }
        try {
            ByteBuffer buffer = ByteBuffer.wrap(Base64.getDecoder().decode(storedText));
            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("복호화에 실패했습니다 (키가 다르거나 데이터가 손상됨)", e);
        }
    }
}