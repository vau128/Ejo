package com.example.librarydashboard.service;

import com.example.librarydashboard.dto.AuthLoginRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {

    public Map<String, Object> login(AuthLoginRequest request) {
        if (!"admin".equals(request.username()) || !"admin123".equals(request.password())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        Map<String, Object> user = new LinkedHashMap<>();
        user.put("name", "중앙도서관 관리자");
        user.put("role", "ADMIN");
        user.put("username", request.username());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("token", UUID.randomUUID().toString());
        response.put("user", user);
        return response;
    }
}
