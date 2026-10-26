package com.skalez.global.storage;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/** 확장자가 아니라 파일 앞부분으로 이미지 종류를 판별한다 */
class ImageStorageTest {

    @Test
    void detectsImageTypesByContent() {
        assertThat(ImageStorage.imageExtension(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0})).isEqualTo("jpg");
        assertThat(ImageStorage.imageExtension(new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A})).isEqualTo("png");
        assertThat(ImageStorage.imageExtension("GIF89a".getBytes(StandardCharsets.US_ASCII))).isEqualTo("gif");
        assertThat(ImageStorage.imageExtension("RIFF0000WEBP".getBytes(StandardCharsets.US_ASCII))).isEqualTo("webp");
    }

    @Test
    void rejectsNonImages() {
        assertThat(ImageStorage.imageExtension("<svg onload=alert(1)>".getBytes(StandardCharsets.UTF_8))).isNull();
        assertThat(ImageStorage.imageExtension("<html>".getBytes(StandardCharsets.UTF_8))).isNull();
        assertThat(ImageStorage.imageExtension(new byte[0])).isNull();
    }
}
