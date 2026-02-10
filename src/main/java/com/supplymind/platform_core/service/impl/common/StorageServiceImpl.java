package com.supplymind.platform_core.service.impl.common;

import com.supplymind.platform_core.common.util.StoragePaths;
import com.supplymind.platform_core.service.common.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.File;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StorageServiceImpl implements StorageService {

    private final S3Client s3Client;
    private final S3Presigner presigner;

    @Value("${b2.bucket}")
    private String bucket;

    @Value("${b2.presign.put.minutes:15}")
    private long presignPutMinutes;

    @Value("${b2.presign.get.minutes:30}")
    private long presignGetMinutes;

    @Override
    public String buildObjectKey(String category, Long ownerId, String fileName) {
        String safeName = sanitizeFilename(fileName);
        String ext = getExtension(safeName);

        String prefix = switch (category) {
            case "product-image" -> StoragePaths.PRODUCT_IMAGES + ownerId + "/";
            case "return-photo"  -> StoragePaths.RETURN_PHOTOS + ownerId + "/";
            case "invoice"       -> StoragePaths.INVOICES + ownerId + "/";
            case "signature"     -> StoragePaths.SIGNATURES + ownerId + "/";
            default -> throw new IllegalArgumentException("Unknown category: " + category);
        };

        return prefix + UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);
    }

    @Override
    public String presignPutUrl(String objectKey, String contentType) {
        PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType(contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .build();

        PutObjectPresignRequest presignReq = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(presignPutMinutes))
                .putObjectRequest(putReq)
                .build();

        return presigner.presignPutObject(presignReq).url().toString();
    }

    @Override
    public String presignGetUrl(String objectKey) {
        GetObjectRequest getReq = GetObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();

        GetObjectPresignRequest presignReq = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(presignGetMinutes))
                .getObjectRequest(getReq)
                .build();

        return presigner.presignGetObject(presignReq).url().toString();
    }

    @Override
    public String uploadFile(String objectKey, File file, String contentType) {
        PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType(contentType)
                .build();

        s3Client.putObject(putReq, file.toPath());

        return presignGetUrl(objectKey);
    }

    private String sanitizeFilename(String name) {
        if (name == null) return "file";
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String getExtension(String name) {
        int i = name.lastIndexOf('.');
        if (i < 0 || i == name.length() - 1) return "";
        return name.substring(i + 1).toLowerCase();
    }
}
