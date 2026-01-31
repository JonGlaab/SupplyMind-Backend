package com.supplymind.platform_core.service.impl.common;

import com.supplymind.platform_core.exception.NotFoundException;
import com.supplymind.platform_core.service.common.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StorageServiceImpl implements StorageService {

    private final S3Client s3;

    @Value("${b2.bucket}")
    private String bucket;

    @Override
    public String upload(MultipartFile file, String folder) {
        try {
            String safeName = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
            String key = folder + "/" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID() + "-" + safeName;

            PutObjectRequest putReq = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3.putObject(putReq, RequestBody.fromBytes(file.getBytes()));
            return key;

        } catch (IOException e) {
            throw new RuntimeException("Upload failed: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] download(String key) {
        try {
            GetObjectRequest getReq = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            ResponseBytes<GetObjectResponse> bytes = s3.getObjectAsBytes(getReq);
            return bytes.asByteArray();

        } catch (NoSuchKeyException e) {
            throw new NotFoundException("File not found: " + key);
        }
    }

    @Override
    public void delete(String key) {
        try {
            DeleteObjectRequest delReq = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            s3.deleteObject(delReq);
        } catch (S3Exception e) {
            throw new RuntimeException("Delete failed: " + e.getMessage(), e);
        }
    }
}

