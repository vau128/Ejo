package com.example.librarydashboard.dto.seat;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SquattingThresholdUpdateResponse(
        String message,
        @JsonProperty("threshold_minutes")
        Integer thresholdMinutes
) {
}
