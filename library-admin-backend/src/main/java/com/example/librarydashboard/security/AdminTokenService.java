package com.example.librarydashboard.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AdminTokenService {

    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final String signingSecret;
    private final long expirationSeconds;

    public AdminTokenService(
            ObjectMapper objectMapper,
            @Value("${app.auth.admin-jwt-secret:change-this-admin-jwt-secret-in-env}") String signingSecret,
            @Value("${app.auth.admin-token-expiration-seconds:43200}") long expirationSeconds
    ) {
        this.objectMapper = objectMapper;
        this.signingSecret = signingSecret;
        this.expirationSeconds = expirationSeconds;
    }

    public String issueAdminToken(String username) {
        long issuedAt = Instant.now().getEpochSecond();
        long expiresAt = issuedAt + expirationSeconds;

        Map<String, Object> header = Map.of(
                "alg", "HS256",
                "typ", "JWT"
        );
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", username);
        payload.put("role", "ADMIN");
        payload.put("iat", issuedAt);
        payload.put("exp", expiresAt);

        String encodedHeader = encodeJson(header);
        String encodedPayload = encodeJson(payload);
        String signature = sign(encodedHeader + "." + encodedPayload);
        return encodedHeader + "." + encodedPayload + "." + signature;
    }

    public Map<String, Object> parseAndValidate(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("잘못된 관리자 토큰 형식입니다.");
        }

        String signingInput = parts[0] + "." + parts[1];
        String expectedSignature = sign(signingInput);
        if (!MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                parts[2].getBytes(StandardCharsets.UTF_8)
        )) {
            throw new IllegalArgumentException("관리자 토큰 서명이 올바르지 않습니다.");
        }

        Map<String, Object> payload = decodeJson(parts[1]);
        if (!"ADMIN".equals(String.valueOf(payload.get("role")))) {
            throw new IllegalArgumentException("관리자 권한이 없는 토큰입니다.");
        }

        long expiresAt = toLong(payload.get("exp"));
        if (Instant.now().getEpochSecond() >= expiresAt) {
            throw new IllegalArgumentException("관리자 토큰이 만료되었습니다.");
        }

        return payload;
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            return URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (Exception exception) {
            throw new IllegalStateException("관리자 토큰 인코딩에 실패했습니다.", exception);
        }
    }

    private Map<String, Object> decodeJson(String encodedValue) {
        try {
            return objectMapper.readValue(URL_DECODER.decode(encodedValue), MAP_TYPE);
        } catch (Exception exception) {
            throw new IllegalArgumentException("관리자 토큰 본문을 읽을 수 없습니다.", exception);
        }
    }

    private String sign(String signingInput) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return URL_ENCODER.encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("관리자 토큰 서명에 실패했습니다.", exception);
        }
    }

    private long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }
}
