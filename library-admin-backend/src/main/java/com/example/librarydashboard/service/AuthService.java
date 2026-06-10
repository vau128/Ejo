package com.example.librarydashboard.service;

import com.example.librarydashboard.dto.AuthLoginRequest;
import com.example.librarydashboard.security.AdminTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AuthService {

    private final AdminTokenService adminTokenService;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;
    private final String adminPasswordHash;
    private final long adminTokenExpirationSeconds;

    public AuthService(
            AdminTokenService adminTokenService,
            PasswordEncoder passwordEncoder,
            @Value("${app.auth.admin-username:admin}") String adminUsername,
            @Value("${app.auth.admin-password:admin123}") String adminPassword,
            @Value("${app.auth.admin-password-hash:}") String adminPasswordHash,
            @Value("${app.auth.admin-token-expiration-seconds:43200}") long adminTokenExpirationSeconds
    ) {
        this.adminTokenService = adminTokenService;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.adminPasswordHash = adminPasswordHash;
        this.adminTokenExpirationSeconds = adminTokenExpirationSeconds;
    }

    public Map<String, Object> login(AuthLoginRequest request) {
        if (!isValidAdminCredentials(request.username(), request.password())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        Map<String, Object> user = new LinkedHashMap<>();
        user.put("name", "중앙도서관 관리자");
        user.put("role", "ADMIN");
        user.put("username", adminUsername);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("token", adminTokenService.issueAdminToken(adminUsername));
        response.put("expiresAt", Instant.now().plusSeconds(adminTokenExpirationSeconds).toString());
        response.put("user", user);
        return response;
    }

    private boolean isValidAdminCredentials(String username, String password) {
        if (!adminUsername.equals(username)) {
            return false;
        }
        if (adminPasswordHash != null && !adminPasswordHash.isBlank()) {
            return passwordEncoder.matches(password, adminPasswordHash);
        }
        return adminPassword.equals(password);
    }
}
