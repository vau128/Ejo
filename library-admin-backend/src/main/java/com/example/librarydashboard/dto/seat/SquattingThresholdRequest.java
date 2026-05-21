package com.example.librarydashboard.dto.seat;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SquattingThresholdRequest(
        @NotNull(message = "threshold_minutes 또는 minutes는 필수입니다.")
        @Min(value = 10, message = "threshold_minutes는 10 이상이어야 합니다.")
        @Max(value = 240, message = "threshold_minutes는 240 이하여야 합니다.")
        @JsonAlias({"minutes", "thresholdMinutes"})
        @JsonProperty("threshold_minutes")
        Integer thresholdMinutes
) {
}
