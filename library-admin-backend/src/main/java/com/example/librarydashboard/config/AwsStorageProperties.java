package com.example.librarydashboard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.aws")
public record AwsStorageProperties(
        boolean enabled,
        String region,
        String s3Bucket,
        String accessKey,
        String secretKey
) {
}
