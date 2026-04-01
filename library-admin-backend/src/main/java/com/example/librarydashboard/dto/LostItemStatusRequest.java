package com.example.librarydashboard.dto;

import jakarta.validation.constraints.NotBlank;

public record LostItemStatusRequest(
        @NotBlank(message = "status는 필수입니다.") String status
) {
}
