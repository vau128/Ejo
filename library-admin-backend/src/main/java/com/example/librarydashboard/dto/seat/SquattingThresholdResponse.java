package com.example.librarydashboard.dto.seat;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SquattingThresholdResponse(
        @JsonProperty("threshold_minutes")
        Integer thresholdMinutes
) {
}
