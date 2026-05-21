package com.example.librarydashboard.dto;

public record AppSettingsRequest(
        boolean pushEnabled,
        boolean seatAlertEnabled,
        boolean warningAlertEnabled
) {
}
