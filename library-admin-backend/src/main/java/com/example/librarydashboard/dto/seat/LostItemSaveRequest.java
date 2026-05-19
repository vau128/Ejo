package com.example.librarydashboard.dto.seat;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record LostItemSaveRequest(
        @NotNull(message = "seat_num은 필수입니다.")
        @JsonProperty("seat_num")
        Integer seatNum,
        @NotBlank(message = "image_url은 필수입니다.")
        @JsonProperty("image_url")
        String imageUrl,
        @NotEmpty(message = "detected_objects는 비어 있을 수 없습니다.")
        @JsonProperty("detected_objects")
        List<String> detectedObjects
) {
}
