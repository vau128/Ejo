package com.example.librarydashboard.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StudentSignupRequest(
        @NotBlank(message = "이름을 입력하세요.")
        String name,
        @NotBlank(message = "학번을 입력하세요.")
        String studentId,
        @Email(message = "올바른 이메일 형식을 입력하세요.")
        @NotBlank(message = "이메일을 입력하세요.")
        String email,
        @Size(min = 6, message = "비밀번호는 6자 이상이어야 합니다.")
        String password,
        boolean agreedToPrivacy
) {
}
