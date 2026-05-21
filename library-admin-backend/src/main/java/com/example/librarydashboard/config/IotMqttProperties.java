package com.example.librarydashboard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.iot.mqtt")
public record IotMqttProperties(
        boolean enabled,
        String brokerUrl,
        String clientIdPrefix,
        String username,
        String password,
        int qos
) {
}
