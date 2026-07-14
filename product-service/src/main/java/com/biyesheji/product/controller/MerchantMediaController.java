package com.biyesheji.product.controller;

import com.biyesheji.constant.UserRole;
import com.biyesheji.dto.R;
import com.biyesheji.exception.BizException;
import com.biyesheji.product.service.MediaStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
public class MerchantMediaController {
    private final MediaStorageService mediaStorageService;

    @PostMapping("/api/merchant/media")
    public R<Map<String, String>> upload(@RequestHeader("X-User-Role") Integer role, @RequestParam("file") MultipartFile file) {
        requireMerchant(role);
        return R.ok(Map.of("url", mediaStorageService.save(file)));
    }

    @DeleteMapping("/api/merchant/media/{filename:.+}")
    public R<Void> delete(@RequestHeader("X-User-Role") Integer role, @PathVariable String filename) {
        requireMerchant(role);
        mediaStorageService.delete(filename);
        return R.ok();
    }

    @GetMapping("/api/media/{filename:.+}")
    public ResponseEntity<Resource> get(@PathVariable String filename) {
        Resource resource = mediaStorageService.load(filename);
        MediaType type = filename.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(type).cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).immutable()).body(resource);
    }

    private void requireMerchant(Integer role) {
        if (!Integer.valueOf(UserRole.OWNER).equals(role) && !Integer.valueOf(UserRole.STAFF).equals(role)) {
            throw new BizException(403, "仅店主或店员可管理媒体");
        }
    }
}
