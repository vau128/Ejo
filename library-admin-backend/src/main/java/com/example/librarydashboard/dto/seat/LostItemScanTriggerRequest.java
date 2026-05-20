package com.example.librarydashboard.dto.seat;

import jakarta.validation.constraints.NotBlank;

public record LostItemScanTriggerRequest(
        @NotBlank(message = "command는 필수입니다.")
        String command
) {
}
