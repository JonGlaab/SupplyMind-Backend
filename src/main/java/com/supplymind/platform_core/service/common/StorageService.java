package com.supplymind.platform_core.service.common;

import java.io.File;

public interface StorageService {
    String buildObjectKey(String category, Long ownerId, String fileName);

    String presignPutUrl(String objectKey, String contentType);

    String presignGetUrl(String objectKey);

    String uploadFile(String objectKey, File file, String contentType);

    void deleteFile(String objectKey);
}
