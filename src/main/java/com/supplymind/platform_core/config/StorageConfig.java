package com.supplymind.platform_core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class StorageConfig {

    @Bean
    public StaticCredentialsProvider b2CredentialsProvider(
            @Value("${b2.key-id}") String keyId,
            @Value("${b2.app-key}") String appKey
    ) {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(keyId, appKey));
    }

    @Bean
    public S3Client s3Client(
            @Value("${b2.region}") String region,
            @Value("${b2.endpoint}") String endpoint,
            StaticCredentialsProvider creds
    ) {
        return S3Client.builder()
                .credentialsProvider(creds)
                .region(Region.of(region))
                .endpointOverride(URI.create(endpoint))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    @Bean
    public S3Presigner s3Presigner(
            @Value("${b2.region}") String region,
            @Value("${b2.endpoint}") String endpoint,
            StaticCredentialsProvider creds
    ) {
        return S3Presigner.builder()
                .credentialsProvider(creds)
                .region(Region.of(region))
                .endpointOverride(URI.create(endpoint))
                .build();
    }
}
