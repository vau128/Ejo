package com.example.librarydashboard.dto.seat;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record PostureUpdateRequest(
        @NotNull(message = "seat_num은 필수입니다.")
        @JsonProperty("seat_num")
        Integer seatNum,
        @NotBlank(message = "posture는 필수입니다.")
        String posture,
        @NotNull(message = "left_pressure는 필수입니다.")
        @JsonProperty("left_pressure")
        Integer leftPressure,
        @NotNull(message = "right_pressure는 필수입니다.")
        @JsonProperty("right_pressure")
        Integer rightPressure,
        @NotNull(message = "back_pressure는 필수입니다.")
        @JsonProperty("back_pressure")
        Integer backPressure,
        @NotNull(message = "timestamp는 필수입니다.")
        OffsetDateTime timestamp
) {
}
