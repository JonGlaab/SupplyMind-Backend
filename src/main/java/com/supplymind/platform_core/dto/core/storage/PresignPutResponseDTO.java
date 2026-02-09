package com.supplymind.platform_core.dto.core.storage;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PresignPutResponseDTO {
    private String objectKey;  // store in DB
    private String putUrl;     // frontend uses this to upload directly to Backblaze
}

