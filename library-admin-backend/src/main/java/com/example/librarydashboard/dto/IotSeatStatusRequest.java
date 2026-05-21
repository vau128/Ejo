package com.example.librarydashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record IotSeatStatusRequest(
        @NotBlank(message = "seat_id는 필수입니다.")
        @JsonProperty("seat_id")
        String seatId,
        @NotBlank(message = "status는 필수입니다.")
        String status,
        @JsonProperty("image_url")
        String imageUrl,
        @JsonProperty("update_time")
        String updateTime
) {
}
