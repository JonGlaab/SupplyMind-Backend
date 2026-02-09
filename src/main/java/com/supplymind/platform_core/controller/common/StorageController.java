package com.supplymind.platform_core.controller.common;

import com.supplymind.platform_core.dto.core.storage.PresignPutRequestDTO;
import com.supplymind.platform_core.dto.core.storage.PresignPutResponseDTO;
import com.supplymind.platform_core.service.common.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/storage")
public class StorageController {

    private final StorageService storageService;

    /**
     * Frontend calls this to get:
     * 1) objectKey to store in DB
     * 2) pre-signed PUT URL to upload the file directly to Backblaze
     */
    @PostMapping("/presign-put")
    public PresignPutResponseDTO presignPut(@RequestBody PresignPutRequestDTO dto) {

        if (dto.getCategory() == null || dto.getCategory().isBlank()) {
            throw new IllegalArgumentException("category is required");
        }
        if (dto.getOwnerId() == null) {
            throw new IllegalArgumentException("ownerId is required");
        }
        if (dto.getFileName() == null || dto.getFileName().isBlank()) {
            throw new IllegalArgumentException("fileName is required");
        }

        String objectKey = storageService.buildObjectKey(dto.getCategory(), dto.getOwnerId(), dto.getFileName());
        String putUrl = storageService.presignPutUrl(objectKey, dto.getContentType());

        return new PresignPutResponseDTO(objectKey, putUrl);
    }

    /**
     * Frontend calls this to get a temporary download/view URL (pre-signed GET).
     */
    @GetMapping("/presign-get")
    public Map<String, String> presignGet(@RequestParam String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("objectKey is required");
        }
        return Map.of("url", storageService.presignGetUrl(objectKey));
    }
}
