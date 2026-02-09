package com.supplymind.platform_core.dto.core.storage;

import lombok.Data;

@Data
public class PresignPutRequestDTO {
    /**
     * Allowed values:
     * product-image | return-photo | invoice | signature
     */
    private String category;

    /**
     * productId / returnId / poId / userId depending on category
     */
    private Long ownerId;

    /**
     * Original file name from the browser
     */
    private String fileName;

    /**
     * MIME type like: image/png, image/jpeg, application/pdf
     */
    private String contentType;
}

