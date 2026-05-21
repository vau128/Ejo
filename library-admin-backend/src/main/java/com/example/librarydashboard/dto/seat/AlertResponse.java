package com.example.librarydashboard.dto.seat;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record AlertResponse(
        @JsonProperty("warning_id")
        Long warningId,
        @JsonProperty("seat_num")
        Integer seatNum,
        String status,
        @JsonProperty("warning_time")
        LocalDateTime warningTime
) {
}
