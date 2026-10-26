package com.skalez.global.storage;

import com.skalez.global.error.BusinessException;
import com.skalez.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 관리자가 올린 이미지 저장 (지금은 서버 폴더). 배포 때 버킷(S3·Lightsail 객체 저장소)으로 바꿀 자리.
 * - 확장자가 아니라 파일 앞부분(매직 넘버)으로 JPG·PNG·WEBP·GIF 인지 확인한다
 * - 파일 이름은 새로 만든다 (원래 이름·경로를 쓰지 않아 다른 폴더로 빠져나갈 수 없다)
 */
@Component
@RequiredArgsConstructor
public class ImageStorage {

    private static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy/MM");

    private final StorageProperties properties;

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

        String relative = LocalDate.now().format(MONTH) + "/" + UUID.randomUUID() + "." + extension;
        Path root = Path.of(properties.dir()).toAbsolutePath().normalize();
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root)) throw new BusinessException(ErrorCode.UPLOAD_INVALID_FILE);
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException e) {
            throw new IllegalStateException("이미지를 저장하지 못했습니다", e);
        }
        return properties.publicUrl() + "/" + relative;
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
