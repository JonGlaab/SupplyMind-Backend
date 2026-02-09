package com.supplymind.platform_core.service.common;

public interface StorageService {
    String buildObjectKey(String category, Long ownerId, String fileName);

    String presignPutUrl(String objectKey, String contentType);

    String presignGetUrl(String objectKey);
}
