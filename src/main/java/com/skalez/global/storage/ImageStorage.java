package com.skalez.global.storage;

import com.skalez.global.error.BusinessException;
import com.skalez.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 관리자가 올린 이미지 저장. skalez.storage.bucket 이 있으면 S3 버킷, 없으면 서버 폴더 (로컬 개발).
 * - 확장자가 아니라 파일 앞부분(매직 넘버)으로 JPG·PNG·WEBP·GIF 인지 확인한다
 * - 파일 이름은 새로 만든다 (원래 이름·경로를 쓰지 않아 다른 폴더로 빠져나갈 수 없다)
 */
@Component
@RequiredArgsConstructor
public class ImageStorage {

    private static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy/MM");

    private final StorageProperties properties;
    private S3Client bucketClient;

    /** 저장하고 고객 화면에서 쓸 주소를 돌려준다 */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_BYTES) {
            throw new BusinessException(ErrorCode.UPLOAD_INVALID_FILE);
        }
        String extension;
        try (InputStream in = file.getInputStream()) {
            extension = imageExtension(in.readNBytes(12));
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.UPLOAD_INVALID_FILE);
        }
        if (extension == null) throw new BusinessException(ErrorCode.UPLOAD_INVALID_FILE);

        try {
            return storeBytes(file.getBytes(), extension);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.UPLOAD_INVALID_FILE);
        }
    }

    /** 서버에서 만든 이미지(예: 배경을 지운 PNG)를 저장한다. extension 은 jpg·png·gif·webp */
    public String storeBytes(byte[] bytes, String extension) {
        String relative = LocalDate.now().format(MONTH) + "/" + UUID.randomUUID() + "." + extension;
        if (properties.useBucket()) {
            return storeInBucket(bytes, relative, extension);
        }
        Path root = Path.of(properties.dir()).toAbsolutePath().normalize();
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root)) throw new BusinessException(ErrorCode.UPLOAD_INVALID_FILE);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
        } catch (IOException e) {
            throw new IllegalStateException("이미지를 저장하지 못했습니다", e);
        }
        return properties.publicUrl() + "/" + relative;
    }

    /** S3 버킷에 올린다. 파일 이름이 매번 새로 생겨서 브라우저가 1년 캐시해도 된다 */
    private String storeInBucket(byte[] bytes, String key, String extension) {
        String contentType = switch (extension) {
            case "jpg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            default -> "image/webp";
        };
        try {
            bucketClient().putObject(
                    PutObjectRequest.builder()
                            .bucket(properties.bucket())
                            .key(key)
                            .contentType(contentType)
                            .cacheControl("public, max-age=31536000, immutable")
                            .build(),
                    RequestBody.fromBytes(bytes));
        } catch (RuntimeException e) {
            throw new IllegalStateException("이미지를 버킷에 저장하지 못했습니다", e);
        }
        return properties.publicUrl() + "/" + key;
    }

    /** 처음 쓸 때 한 번 만든다 (버킷을 안 쓰는 로컬에서는 AWS 접속 준비를 아예 하지 않는다) */
    private synchronized S3Client bucketClient() {
        if (bucketClient == null) {
            bucketClient = S3Client.builder()
                    .region(Region.of(properties.region()))
                    .httpClientBuilder(UrlConnectionHttpClient.builder())
                    .build(); // 접속 키는 환경변수 AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY 에서 읽는다
        }
        return bucketClient;
    }

    /** 파일 앞부분으로 이미지 종류를 판별한다. 이미지가 아니면 null */
    static String imageExtension(byte[] head) {
        if (head.length >= 3 && (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF) return "jpg";
        if (head.length >= 8 && (head[0] & 0xFF) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G') return "png";
        if (head.length >= 6 && head[0] == 'G' && head[1] == 'I' && head[2] == 'F' && head[3] == '8') return "gif";
        if (head.length >= 12 && head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P') return "webp";
        return null;
    }
}
