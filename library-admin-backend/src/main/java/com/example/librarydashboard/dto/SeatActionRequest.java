package com.example.librarydashboard.dto;

import jakarta.validation.constraints.NotBlank;

public record SeatActionRequest(
        @NotBlank(message = "seatId는 필수입니다.") String seatId
) {
}
