package com.example.librarydashboard.dto.seat;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record SeatSummaryResponse(
        @JsonProperty("seat_num")
        Integer seatNum,
        String location,
        String status,
        @JsonProperty("checked_in")
        boolean checkedIn,
        String posture,
        @JsonProperty("left_pressure")
        Integer leftPressure,
        @JsonProperty("right_pressure")
        Integer rightPressure,
        @JsonProperty("back_pressure")
        Integer backPressure,
        @JsonProperty("updated_at")
        LocalDateTime updatedAt
) {
}
