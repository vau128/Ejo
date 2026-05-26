package com.example.librarydashboard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.iot")
public record IotProperties(
        boolean enabled,
        String baseUrl,
        String apiKey,
        int requestTimeoutMs,
        int heartbeatThresholdSeconds
) {
}
