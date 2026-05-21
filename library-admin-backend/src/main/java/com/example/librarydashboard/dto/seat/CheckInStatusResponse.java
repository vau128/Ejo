package com.example.librarydashboard.dto.seat;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CheckInStatusResponse(
        @JsonProperty("seat_num")
        Integer seatNum,
        @JsonProperty("is_checked_in")
        boolean isCheckedIn
) {
}
