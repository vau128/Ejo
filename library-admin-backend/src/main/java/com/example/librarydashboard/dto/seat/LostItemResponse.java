package com.example.librarydashboard.dto.seat;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

public record LostItemResponse(
        Long id,
        @JsonProperty("seat_num")
        Integer seatNum,
        @JsonProperty("image_url")
        String imageUrl,
        @JsonProperty("detected_objects")
        List<String> detectedObjects,
        @JsonProperty("created_at")
        LocalDateTime createdAt
) {
}
