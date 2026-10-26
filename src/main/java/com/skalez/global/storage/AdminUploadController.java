package com.skalez.global.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 관리자 이미지 업로드 (배너·상품 사진). 응답 url 을 배너·상품의 이미지 주소로 저장한다 */
@RestController
@RequiredArgsConstructor
public class AdminUploadController {

    private final ImageStorage imageStorage;

    public record UploadResponse(String url) {}

    @PostMapping(value = "/api/admin/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public UploadResponse upload(@RequestParam("file") MultipartFile file) {
        return new UploadResponse(imageStorage.store(file));
    }
}
