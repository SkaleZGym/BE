package com.skalez.global.storage;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 관리자 이미지 업로드 (배너·상품 사진). 응답 url 을 배너·상품의 이미지 주소로 저장한다 */
@RestController
@RequiredArgsConstructor
public class AdminUploadController {

    private final ImageStorage imageStorage;
    private final BackgroundRemover backgroundRemover;

    public record UploadResponse(String url) {}

    public record RemoveBackgroundRequest(@NotBlank String imageUrl) {}

    /** alreadyTransparent=true 면 이미 배경이 투명해서 원래 주소를 그대로 돌려준 것 */
    public record RemoveBackgroundResponse(String url, boolean alreadyTransparent) {}

    @PostMapping(value = "/api/admin/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public UploadResponse upload(@RequestParam("file") MultipartFile file) {
        return new UploadResponse(imageStorage.store(file));
    }

    /** 히어로 배너용: 상품 사진의 단색 배경을 지운 투명 PNG 를 새로 저장한다 (원본은 그대로) */
    @PostMapping("/api/admin/uploads/remove-background")
    public RemoveBackgroundResponse removeBackground(@Valid @RequestBody RemoveBackgroundRequest request) {
        byte[] png = backgroundRemover.removeFrom(request.imageUrl());
        if (png == null) {
            return new RemoveBackgroundResponse(request.imageUrl(), true);
        }
        return new RemoveBackgroundResponse(imageStorage.storeBytes(png, "png"), false);
    }
}
