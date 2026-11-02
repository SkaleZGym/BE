package com.skalez.global.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 관리자 이미지 저장소.
 * dir: 파일을 저장할 서버 폴더 (로컬 저장소)
 * publicUrl: 고객 화면에서 쓰는 사진 주소 앞부분 (예: https://api.skalez.com/files, 버킷이면 https://img.skalez.com)
 * bucket·region: S3 버킷 이름과 지역. bucket 이 비어 있으면 위 서버 폴더에 저장한다.
 *   접속 키는 환경변수 AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY (코드·설정 파일에 두지 않는다)
 */
@ConfigurationProperties("skalez.storage")
public record StorageProperties(String dir, String publicUrl, String bucket, String region) {

    public boolean useBucket() {
        return bucket != null && !bucket.isBlank();
    }
}
