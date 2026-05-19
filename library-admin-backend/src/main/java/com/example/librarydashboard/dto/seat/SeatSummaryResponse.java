package com.example.librarydashboard.dto.seat;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record SeatSummaryResponse(
        @JsonProperty("seat_num")
        Integer seatNum,
        String status,
        Integer pressure,
        @JsonProperty("updated_at")
        LocalDateTime updatedAt
) {
}
