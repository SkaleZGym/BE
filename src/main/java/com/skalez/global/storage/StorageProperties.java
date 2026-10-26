package com.skalez.global.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 관리자 이미지 저장소.
 * dir: 파일을 저장할 서버 폴더 (로컬 저장소)
 * publicUrl: 고객 화면에서 쓰는 사진 주소 앞부분 (예: https://api.skalez.com/files)
 */
@ConfigurationProperties("skalez.storage")
public record StorageProperties(String dir, String publicUrl) {
}
