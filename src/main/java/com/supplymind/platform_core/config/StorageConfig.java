package com.supplymind.platform_core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
public class StorageConfig {

    @Value("${b2.key-id}")
    private String keyId;

    @Value("${b2.app-key}")
    private String appKey;

    @Value("${b2.endpoint}")
    private String endpoint;

    @Bean
    public S3Client b2S3Client() {
        return S3Client.builder()
                // Backblaze uses S3-compatible endpoint (not AWS)
                .endpointOverride(URI.create(endpoint))
                // Region is required by SDK but ignored by endpointOverride
                .region(Region.US_EAST_1)
                .credentialsProvider(
                        StaticCredentialsProvider.create(AwsBasicCredentials.create(keyId, appKey))
                )
                // required for many S3-compatible providers
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .httpClient(UrlConnectionHttpClient.create())
                .build();
    }
}

