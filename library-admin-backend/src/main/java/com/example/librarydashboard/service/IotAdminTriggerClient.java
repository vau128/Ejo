package com.example.librarydashboard.service;

import com.example.librarydashboard.config.IotProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class IotAdminTriggerClient {

    private final IotProperties properties;
    private final HttpClient httpClient;

    public IotAdminTriggerClient(IotProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMillis()))
                .build();
    }

    public void triggerLostItemScan(String command) {
        String baseUrl = properties.baseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("IOT_BASE_URL is not configured");
        }

        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(normalizedBaseUrl + "/admin/lost-item-scan"))
                .timeout(Duration.ofMillis(timeoutMillis()))
                .header("Content-Type", "application/json")
                .header("X-IoT-Api-Key", defaultString(properties.apiKey()))
                .POST(HttpRequest.BodyPublishers.ofString("{\"command\":\"" + command + "\"}"))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("IoT HTTP trigger failed with status " + response.statusCode());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to call IoT HTTP trigger", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to call IoT HTTP trigger", exception);
        }
    }

    private int timeoutMillis() {
        return properties.requestTimeoutMs() > 0 ? properties.requestTimeoutMs() : 3000;
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }
}
