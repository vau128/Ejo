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

import java.net.URI;
import java.time.Duration;

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

        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                properties.accessKey(),
                properties.secretKey()
        );

        try (S3Presigner presigner = S3Presigner.builder()
                .region(Region.of(properties.region()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build()) {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(properties.s3Bucket())
                    .key(storedValue)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(15))
                    .getObjectRequest(getObjectRequest)
                    .build();

            return presigner.presignGetObject(presignRequest).url().toString();
        } catch (RuntimeException exception) {
            return storedValue;
        }
    }

    private boolean looksLikeAbsoluteUrl(String value) {
        try {
            URI uri = URI.create(value);
            return uri.getScheme() != null && uri.getHost() != null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
