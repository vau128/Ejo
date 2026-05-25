package com.example.librarydashboard.adapter.out;

import com.example.librarydashboard.config.AwsStorageProperties;
import com.example.librarydashboard.port.out.ObjectStorageUrlResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.net.URI;
import java.nio.charset.StandardCharsets;

@Component
@ConditionalOnProperty(prefix = "app.aws", name = "enabled", havingValue = "true")
public class S3ObjectStorageUrlResolver implements ObjectStorageUrlResolver {

    private final AwsStorageProperties properties;

    public S3ObjectStorageUrlResolver(AwsStorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public String resolveReadUrl(String storedValue) {
        if (storedValue == null || storedValue.isBlank()) {
            return storedValue;
        }
        if (looksLikeAbsoluteUrl(storedValue)) {
            return storedValue;
        }

        return buildPublicObjectUrl(storedValue);
    }

    private boolean looksLikeAbsoluteUrl(String value) {
        try {
            URI uri = URI.create(value);
            return uri.getScheme() != null && uri.getHost() != null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private String buildPublicObjectUrl(String objectKey) {
        String normalizedKey = objectKey.startsWith("/") ? objectKey.substring(1) : objectKey;
        String encodedKey = encodePath(normalizedKey);
        return "https://%s.s3.%s.amazonaws.com/%s".formatted(
                properties.s3Bucket(),
                properties.region(),
                encodedKey
        );
    }

    private String encodePath(String path) {
        String[] segments = path.split("/");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                builder.append('/');
            }
            builder.append(URLEncoder.encode(segments[i], StandardCharsets.UTF_8).replace("+", "%20"));
        }
        return builder.toString();
    }
}
