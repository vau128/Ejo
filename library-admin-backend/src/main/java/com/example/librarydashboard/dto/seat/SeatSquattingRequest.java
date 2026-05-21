package com.example.librarydashboard.dto.seat;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SeatSquattingRequest(
        @NotNull(message = "seat_num은 필수입니다.")
        @JsonProperty("seat_num")
        Integer seatNum,
        @NotBlank(message = "status는 필수입니다.")
        String status
) {
}
