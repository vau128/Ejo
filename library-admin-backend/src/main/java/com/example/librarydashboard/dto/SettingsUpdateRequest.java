package com.example.librarydashboard.dto;

public record SettingsUpdateRequest(
        boolean pushAlertsEnabled,
        boolean smsAlertsEnabled,
        String quietHoursStart,
        String quietHoursEnd,
        boolean autoReleaseEnabled,
        boolean lostItemAutoRegisterEnabled,
        int vacantSeatThresholdMinutes,
        int objectDetectionThresholdMinutes,
        int dashboardRefreshSeconds,
        int sensorDelayThresholdSeconds,
        String libraryMode
) {
}
