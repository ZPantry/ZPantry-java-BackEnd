package com.zpantry.media.service;

import org.springframework.web.multipart.MultipartFile;

public interface MediaStoragePort {
    UploadResult upload(MultipartFile file, String folder);

    void delete(String publicId);

    record UploadResult(String publicId, String url, String secureUrl, String resourceType, String format,
                        Integer width, Integer height) {
    }
}
