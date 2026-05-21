package com.example.librarydashboard.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record StudentLoginRequest(
        @Email(message = "올바른 이메일 형식을 입력하세요.")
        @NotBlank(message = "이메일을 입력하세요.")
        String email,
        @NotBlank(message = "비밀번호를 입력하세요.")
        String password
) {
}
