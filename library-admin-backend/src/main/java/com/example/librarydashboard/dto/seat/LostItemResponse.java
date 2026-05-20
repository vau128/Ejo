package com.example.librarydashboard.dto.seat;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
public record LostItemResponse(
        @JsonProperty("item_id")
        Long itemId,
        @JsonProperty("seat_num")
        Integer seatNum,
        String category,
        @JsonProperty("image_url")
        String imageUrl,
        @JsonProperty("detected_time")
        LocalDateTime detectedTime,
        String status
) {
}
