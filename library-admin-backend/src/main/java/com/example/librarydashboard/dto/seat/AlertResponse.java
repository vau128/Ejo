package com.example.librarydashboard.dto.seat;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record AlertResponse(
        Long id,
        @JsonProperty("seat_num")
        Integer seatNum,
        @JsonProperty("alert_type")
        String alertType,
        String status,
        String message,
        @JsonProperty("created_at")
        LocalDateTime createdAt
) {
}
