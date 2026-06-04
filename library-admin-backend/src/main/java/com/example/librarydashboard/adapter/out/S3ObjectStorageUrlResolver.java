package com.example.librarydashboard.adapter.out;

import com.example.librarydashboard.config.AwsStorageProperties;
import com.example.librarydashboard.port.out.ObjectStorageUrlResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URLEncoder;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;

@Component
@ConditionalOnProperty(prefix = "app.aws", name = "enabled", havingValue = "true")
public class S3ObjectStorageUrlResolver implements ObjectStorageUrlResolver {

    private final AwsStorageProperties properties;
    private final S3Presigner presigner;

    public S3ObjectStorageUrlResolver(AwsStorageProperties properties) {
        this.properties = properties;
        this.presigner = S3Presigner.builder()
                .region(Region.of(properties.region()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.accessKey(), properties.secretKey())
                ))
                .build();
    }

    @Override
    public String resolveReadUrl(String storedValue) {
        if (storedValue == null || storedValue.isBlank()) {
            return storedValue;
        }
        if (looksLikeAbsoluteUrl(storedValue)) {
            String objectKey = extractManagedObjectKey(storedValue);
            if (objectKey != null) {
                return buildPresignedObjectUrl(objectKey);
            }
            return storedValue;
        }

        return buildPresignedObjectUrl(storedValue);
    }

    private boolean looksLikeAbsoluteUrl(String value) {
        try {
            URI uri = URI.create(value);
            return uri.getScheme() != null && uri.getHost() != null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private String buildPresignedObjectUrl(String objectKey) {
        String normalizedKey = objectKey.startsWith("/") ? objectKey.substring(1) : objectKey;
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(properties.s3Bucket())
                .key(normalizedKey)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(1))
                .getObjectRequest(getObjectRequest)
                .build();
        return presigner.presignGetObject(presignRequest).url().toString();
    }

    private String extractManagedObjectKey(String storedValue) {
        try {
            URI uri = URI.create(storedValue);
            String host = uri.getHost();
            if (host == null) {
                return null;
            }

            String bucket = properties.s3Bucket();
            String normalizedHost = host.toLowerCase(Locale.ROOT);
            String virtualHostedPrefix = (bucket + ".s3.").toLowerCase(Locale.ROOT);
            String pathStyleSegment = "/" + bucket + "/";

            if (normalizedHost.startsWith(virtualHostedPrefix)) {
                String path = uri.getPath();
                return path == null ? null : trimLeadingSlash(path);
            }

            String path = uri.getPath();
            if (path != null && path.startsWith(pathStyleSegment)) {
                return path.substring(pathStyleSegment.length());
            }

            return null;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String trimLeadingSlash(String value) {
        return value.startsWith("/") ? value.substring(1) : value;
    }

    @SuppressWarnings("unused")
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
