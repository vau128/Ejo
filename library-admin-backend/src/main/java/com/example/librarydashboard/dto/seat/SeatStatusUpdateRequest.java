package com.example.librarydashboard.dto.seat;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record SeatStatusUpdateRequest(
        @NotNull(message = "seat_num은 필수입니다.")
        @JsonProperty("seat_num")
        Integer seatNum,
        @NotNull(message = "pressure는 필수입니다.")
        Integer pressure,
        @NotNull(message = "timestamp는 필수입니다.")
        OffsetDateTime timestamp
) {
}
