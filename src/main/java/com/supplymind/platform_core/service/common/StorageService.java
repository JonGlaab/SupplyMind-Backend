package com.supplymind.platform_core.service.common;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    String upload(MultipartFile file, String folder);
    byte[] download(String key);
    void delete(String key);
}

