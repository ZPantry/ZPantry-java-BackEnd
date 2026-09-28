package com.zpantry.media.api;

import com.zpantry.common.api.ApiResponse;
import com.zpantry.media.domain.MediaAssetEntity;
import com.zpantry.media.persistence.MediaAssetRepository;
import com.zpantry.media.service.MediaStoragePort;

import java.time.Instant;
import java.util.UUID;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/media")
public class MediaController {
    private final MediaStoragePort storage;
    private final MediaAssetRepository assets;

    public MediaController(MediaStoragePort s, MediaAssetRepository a) {
        storage = s;
        assets = a;
    }

    private static <T> ApiResponse<T> ok(T d, String m) {
        return new ApiResponse<>(true, m, d, null, "", Instant.now());
    }

    @PostMapping(path = "/upload", consumes = "multipart/form-data")
    public ApiResponse<String> upload(@RequestPart("file") MultipartFile file, @RequestParam(required = false) UUID recipeId, @RequestParam(required = false) UUID ingredientId) {
        var u = storage.upload(file, "zpantry");
        assets.save(new MediaAssetEntity(recipeId, ingredientId, u.publicId(), u.url(), u.secureUrl(), u.resourceType(), u.format(), u.width(), u.height()));
        return ok(u.secureUrl(), "Upload successful.");
    }

    @DeleteMapping
    public ApiResponse<Object> delete(@RequestParam String publicId) {
        storage.delete(publicId);
        assets.findByPublicIdAndDeletedFalse(publicId).ifPresent(MediaAssetEntity::softDelete);
        return ok(null, "Media deleted.");
    }
}
