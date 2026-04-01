package com.example.librarydashboard.service;

import com.example.librarydashboard.dto.SettingsUpdateRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DashboardService {

    private static final DateTimeFormatter HISTORY_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm", Locale.KOREAN);

    private final List<Map<String, Object>> seats = new ArrayList<>();
    private final List<Map<String, Object>> alertHistory = new ArrayList<>();
    private final List<Map<String, Object>> alertRules = new ArrayList<>();
    private final List<Map<String, Object>> lostItems = new ArrayList<>();
    private final List<Map<String, Object>> devices = new ArrayList<>();
    private final List<Map<String, Object>> sensorLogs = new ArrayList<>();
    private final Map<String, Object> settings = new LinkedHashMap<>();

    public DashboardService() {
        seedSeats();
        seedAlertHistory();
        seedAlertRules();
        seedLostItems();
        seedDevices();
        seedSensorLogs();
        seedSettings();
    }

    public Map<String, Object> getOverview() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("summary", buildSeatSummary(seats));
        response.put("actionQueue", buildActionQueue().stream().limit(4).toList());
        response.put("recentAlertHistory", alertHistory.stream().limit(4).toList());

        Map<String, Object> zonePreview = new LinkedHashMap<>();
        zonePreview.put("totalSeats", seats.size());
        zonePreview.put("occupiedSeats", countByStatus("OCCUPIED"));
        zonePreview.put("abnormalSeats", countAbnormalSeats());
        zonePreview.put("seats", seats.stream().limit(16).toList());
        response.put("zonePreview", zonePreview);

        response.put("lostItemsPreview", lostItems.stream().limit(3).toList());

        Map<String, Object> systemPreview = new LinkedHashMap<>();
        systemPreview.put("connectedSensors", buildSystemSummary().get("sensorConnected"));
        systemPreview.put("cameraOnline", buildSystemSummary().get("cameraOnline"));
        systemPreview.put("delayedFeeds", buildSystemSummary().get("dataDelay"));
        systemPreview.put("devices", devices.stream().limit(3).toList());
        response.put("systemPreview", systemPreview);

        return response;
    }

    public Map<String, Object> getActions() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("pendingWarnings", buildActionQueue().size());
        summary.put("pendingReleases", buildActionQueue().stream().filter(item -> !"처리 완료".equals(item.get("status"))).count());
        summary.put("resolvedToday", 9);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("summary", summary);
        response.put("queue", buildActionQueue());
        return response;
    }

    public Map<String, Object> sendWarning(String seatId) {
        Map<String, Object> seat = findSeat(seatId);
        seat.put("actionStatus", "경고 전송");
        seat.put("notes", "관리자 경고 발송 완료");
        addHistory(seatId, "경고 전송", "앱 푸시", "전송 완료", "좌석 상태 이상으로 경고 메시지를 발송했습니다.");
        addSensorLog("ALERT_PUSH", seatId, "edge-rpi-03", "warn", "관리자 경고 메시지 전송", "정상");
        return message("경고를 전송했습니다.");
    }

    public Map<String, Object> releaseSeat(String seatId) {
        Map<String, Object> seat = findSeat(seatId);
        seat.put("status", "AVAILABLE");
        seat.put("statusLabel", statusLabel("AVAILABLE"));
        seat.put("abnormal", false);
        seat.put("durationMinutes", 0);
        seat.put("actionStatus", "상태 해제");
        seat.put("issueType", "정상 상태 복구");
        seat.put("notes", "관리자 확인 후 사용 가능 처리");
        seat.put("lastUpdated", HISTORY_FORMATTER.format(LocalDateTime.now()));
        addHistory(seatId, "상태 해제", "관리자 처리", "전송 완료", "관리자가 좌석 상태를 사용 가능으로 복구했습니다.");
        addSensorLog("SEAT_RELEASE", seatId, "edge-rpi-02", "release", "관리자 상태 해제 처리", "정상");
        return message("좌석 상태를 해제했습니다.");
    }

    public Map<String, Object> resolveIssue(String seatId) {
        Map<String, Object> seat = findSeat(seatId);
        seat.put("abnormal", false);
        seat.put("actionStatus", "처리 완료");
        seat.put("notes", "현장 확인 완료");
        if (!Objects.equals(seat.get("status"), "AVAILABLE")) {
            seat.put("status", "OCCUPIED");
            seat.put("statusLabel", statusLabel("OCCUPIED"));
        }
        addHistory(seatId, "처리 완료", "관리자 처리", "전송 완료", "비정상 상태 확인 후 처리 완료로 변경했습니다.");
        addSensorLog("ISSUE_RESOLVED", seatId, "edge-rpi-01", "resolved", "현장 점검 후 처리 완료", "정상");
        return message("처리 완료로 변경했습니다.");
    }

    public Map<String, Object> getAlertHistory() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("warningsSent", alertHistory.size());
        summary.put("resolvedAlerts", alertHistory.stream().filter(item -> Objects.equals(item.get("messageType"), "처리 완료")).count());
        summary.put("deliverySuccessRate", "93%");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("summary", summary);
        response.put("history", alertHistory);
        return response;
    }

    public Map<String, Object> getAlertManagement() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("enabledRules", alertRules.stream().filter(rule -> Boolean.TRUE.equals(rule.get("enabled"))).count());
        summary.put("disabledRules", alertRules.stream().filter(rule -> Boolean.FALSE.equals(rule.get("enabled"))).count());
        summary.put("pendingTargets", buildAbnormalSeatRows().size());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("summary", summary);
        response.put("rules", alertRules);
        response.put("targets", buildAbnormalSeatRows().stream().map(row -> mapOf(
                "seatId", row.get("seatId"),
                "currentStatus", row.get("statusLabel"),
                "triggerAt", row.get("detectedAt"),
                "recommendedAction", row.get("actionStatus"),
                "severity", row.get("severity")
        )).toList());
        return response;
    }

    public Map<String, Object> updateAlertRule(String ruleId, boolean enabled) {
        Map<String, Object> rule = alertRules.stream()
                .filter(item -> Objects.equals(item.get("ruleId"), ruleId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "알림 규칙을 찾을 수 없습니다."));

        rule.put("enabled", enabled);
        return message("알림 규칙 상태를 변경했습니다.");
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("peakUsageRate", "84%");
        summary.put("seatTurnover", "3.1회");
        summary.put("abnormalFrequency", "9건/일");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("summary", summary);
        response.put("hourlyUsage", List.of(
                point("09:00", 35), point("10:00", 52), point("11:00", 68), point("12:00", 61),
                point("13:00", 78), point("14:00", 74), point("15:00", 84), point("16:00", 80),
                point("17:00", 72), point("18:00", 59)
        ));
        response.put("turnoverTrend", List.of(
                point("09:00", 1), point("10:00", 2), point("11:00", 2), point("12:00", 3),
                point("13:00", 2), point("14:00", 4), point("15:00", 4), point("16:00", 3),
                point("17:00", 3), point("18:00", 2)
        ));
        response.put("abnormalBreakdown", List.of(
                mapOf("label", "물품 장기 방치", "value", 8, "rate", 80),
                mapOf("label", "장시간 비움", "value", 6, "rate", 60),
                mapOf("label", "센서 지연", "value", 4, "rate", 40),
                mapOf("label", "카메라 재인식 필요", "value", 3, "rate", 30)
        ));
        return response;
    }

    public Map<String, Object> getSettings() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("summary", mapOf(
                "enabledAutomations", countEnabledAutomations(),
                "activeChannels", countActiveChannels(),
                "refreshSeconds", settings.get("dashboardRefreshSeconds")
        ));
        response.put("settings", new LinkedHashMap<>(settings));
        return response;
    }

    public Map<String, Object> updateSettings(SettingsUpdateRequest request) {
        settings.put("pushAlertsEnabled", request.pushAlertsEnabled());
        settings.put("smsAlertsEnabled", request.smsAlertsEnabled());
        settings.put("quietHoursStart", request.quietHoursStart());
        settings.put("quietHoursEnd", request.quietHoursEnd());
        settings.put("autoReleaseEnabled", request.autoReleaseEnabled());
        settings.put("lostItemAutoRegisterEnabled", request.lostItemAutoRegisterEnabled());
        settings.put("vacantSeatThresholdMinutes", request.vacantSeatThresholdMinutes());
        settings.put("objectDetectionThresholdMinutes", request.objectDetectionThresholdMinutes());
        settings.put("dashboardRefreshSeconds", request.dashboardRefreshSeconds());
        settings.put("sensorDelayThresholdSeconds", request.sensorDelayThresholdSeconds());
        settings.put("libraryMode", request.libraryMode());
        addSensorLog("SETTINGS_UPDATED", "ADMIN", "dashboard-web", request.libraryMode(), "관리자 설정이 업데이트되었습니다.", "정상");
        return message("설정을 저장했습니다.");
    }

    public Map<String, Object> getZoneSeats(String status, String search) {
        List<Map<String, Object>> filtered = seats.stream()
                .filter(seat -> status == null || status.isBlank() || Objects.equals(seat.get("status"), status))
                .filter(seat -> search == null || search.isBlank() || String.valueOf(seat.get("seatId")).toLowerCase().contains(search.toLowerCase()))
                .toList();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("zoneName", "3구역");
        summary.put("totalSeats", seats.size());
        summary.put("occupiedSeats", countByStatus("OCCUPIED"));
        summary.put("availableSeats", countByStatus("AVAILABLE"));
        summary.put("abnormalSeats", countAbnormalSeats());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("summary", summary);
        response.put("seats", filtered);
        return response;
    }

    public Map<String, Object> getSeatDetail(String seatId) {
        Map<String, Object> seat = findSeat(seatId);
        Map<String, Object> sensor = new LinkedHashMap<>();
        sensor.put("pressureValue", seat.get("pressureValue"));
        sensor.put("personDetected", seat.get("personDetected"));
        sensor.put("objectDetected", seat.get("objectDetected"));
        sensor.put("cameraConfidence", seat.get("cameraConfidence"));
        sensor.put("gateway", seat.get("gateway"));

        Map<String, Object> response = new LinkedHashMap<>(seat);
        response.put("sensor", sensor);
        response.put("history", List.of(
                mapOf("time", HISTORY_FORMATTER.format(LocalDateTime.now().minusMinutes(18)), "statusLabel", "정상 착석", "reason", "압력 센서와 카메라에서 사용자 착석 감지"),
                mapOf("time", HISTORY_FORMATTER.format(LocalDateTime.now().minusMinutes(10)), "statusLabel", String.valueOf(seat.get("statusLabel")), "reason", String.valueOf(seat.get("issueType"))),
                mapOf("time", HISTORY_FORMATTER.format(LocalDateTime.now().minusMinutes(3)), "statusLabel", "관리자 확인 대기", "reason", String.valueOf(seat.get("notes")))
        ));
        return response;
    }

    public Map<String, Object> getAbnormalSeats() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("abnormalCount", countAbnormalSeats());
        summary.put("objectOnly", countByStatus("OBJECT_ONLY"));
        summary.put("vacantLong", countByStatus("VACANT_LONG"));
        summary.put("sensorDelay", countByStatus("SENSOR_DELAY"));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("summary", summary);
        response.put("breakdown", List.of(
                mapOf("label", "물품 감지", "value", countByStatus("OBJECT_ONLY")),
                mapOf("label", "장시간 비움", "value", countByStatus("VACANT_LONG")),
                mapOf("label", "센서 지연", "value", countByStatus("SENSOR_DELAY"))
        ));
        response.put("rows", buildAbnormalSeatRows());
        return response;
    }

    public Map<String, Object> getLostItems() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("openCount", lostItems.stream().filter(item -> Objects.equals(item.get("status"), "보관 중")).count());
        summary.put("claimedToday", lostItems.stream().filter(item -> Objects.equals(item.get("status"), "인계 완료")).count());
        summary.put("storageCount", lostItems.size());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("summary", summary);
        response.put("items", lostItems);
        return response;
    }

    public Map<String, Object> updateLostItemStatus(String itemId, String status) {
        Map<String, Object> item = lostItems.stream()
                .filter(lostItem -> Objects.equals(lostItem.get("itemId"), itemId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "분실물을 찾을 수 없습니다."));
        item.put("status", status);
        return message("분실물 상태를 변경했습니다.");
    }

    public Map<String, Object> getSystemStatus() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("summary", buildSystemSummary());
        response.put("devices", devices);
        return response;
    }

    public Map<String, Object> getSensorLogs() {
    List<Map<String, Object>> sortedLogs = sensorLogs.stream()
            .sorted(Comparator.comparing((Map<String, Object> item) -> String.valueOf(item.get("timestamp"))).reversed())
            .toList();
    return mapOf("logs", sortedLogs);
}

    private Map<String, Object> buildSystemSummary() {
        long sensorConnected = devices.stream().filter(device -> Objects.equals(device.get("status"), "정상")).count();
        long cameraOnline = devices.stream().filter(device -> Objects.equals(device.get("type"), "Camera") && !Objects.equals(device.get("status"), "오프라인")).count();
        long dataDelay = devices.stream().filter(device -> Objects.equals(device.get("status"), "지연")).count();

        return mapOf(
                "sensorConnected", sensorConnected + "/" + devices.size(),
                "cameraOnline", cameraOnline + "/3",
                "dataDelay", dataDelay + "건"
        );
    }

    private Map<String, Object> buildSeatSummary(List<Map<String, Object>> source) {
        return mapOf(
                "totalSeats", source.size(),
                "occupiedSeats", countByStatus("OCCUPIED"),
                "availableSeats", countByStatus("AVAILABLE"),
                "abnormalSeats", countAbnormalSeats(),
                "alertsToday", alertHistory.size(),
                "openLostItems", lostItems.stream().filter(item -> Objects.equals(item.get("status"), "보관 중")).count()
        );
    }

    private List<Map<String, Object>> buildActionQueue() {
        return seats.stream()
                .filter(seat -> Boolean.TRUE.equals(seat.get("abnormal")))
                .map(seat -> mapOf(
                        "seatId", seat.get("seatId"),
                        "issueType", seat.get("issueType"),
                        "detectedAt", seat.get("detectedAt"),
                        "duration", seat.get("durationMinutes") + "분",
                        "sensorHint", seat.get("sensorHint"),
                        "status", seat.getOrDefault("actionStatus", "대기")
                ))
                .toList();
    }

    private List<Map<String, Object>> buildAbnormalSeatRows() {
        return seats.stream()
                .filter(seat -> Boolean.TRUE.equals(seat.get("abnormal")))
                .map(seat -> mapOf(
                        "seatId", seat.get("seatId"),
                        "statusLabel", seat.get("statusLabel"),
                        "detectedAt", seat.get("detectedAt"),
                        "durationMinutes", seat.get("durationMinutes") + "분",
                        "severity", severityForStatus(String.valueOf(seat.get("status"))),
                        "actionStatus", seat.getOrDefault("actionStatus", "확인 필요")
                ))
                .toList();
    }

    private long countByStatus(String status) {
        return seats.stream().filter(seat -> Objects.equals(seat.get("status"), status)).count();
    }

    private long countAbnormalSeats() {
        return seats.stream().filter(seat -> Boolean.TRUE.equals(seat.get("abnormal"))).count();
    }

    private long countEnabledAutomations() {
        return List.of(
                settings.get("pushAlertsEnabled"),
                settings.get("smsAlertsEnabled"),
                settings.get("autoReleaseEnabled"),
                settings.get("lostItemAutoRegisterEnabled")
        ).stream().filter(Boolean.TRUE::equals).count();
    }

    private long countActiveChannels() {
        return List.of(settings.get("pushAlertsEnabled"), settings.get("smsAlertsEnabled"))
                .stream()
                .filter(Boolean.TRUE::equals)
                .count();
    }

    private String severityForStatus(String status) {
        return switch (status) {
            case "OBJECT_ONLY" -> "주의";
            case "VACANT_LONG" -> "긴급";
            case "SENSOR_DELAY" -> "지연";
            default -> "확인";
        };
    }

    private Map<String, Object> findSeat(String seatId) {
        return seats.stream()
                .filter(seat -> Objects.equals(seat.get("seatId"), seatId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "좌석을 찾을 수 없습니다."));
    }

    private void addHistory(String seatId, String messageType, String channel, String status, String message) {
        alertHistory.add(0, mapOf(
                "id", "AL-" + (1000 + alertHistory.size() + 1),
                "seatId", seatId,
                "studentIdMasked", "2023****",
                "messageType", messageType,
                "channel", channel,
                "createdAt", HISTORY_FORMATTER.format(LocalDateTime.now()),
                "status", status,
                "message", message
        ));
    }

    private void addSensorLog(String eventType, String seatId, String deviceId, String value, String message, String status) {
        sensorLogs.add(0, mapOf(
                "id", UUID.randomUUID().toString(),
                "timestamp", HISTORY_FORMATTER.format(LocalDateTime.now()),
                "deviceId", deviceId,
                "seatId", seatId,
                "eventType", eventType,
                "value", value,
                "message", message,
                "status", status
        ));
    }

    private Map<String, Object> point(String time, int value) {
        return mapOf("time", time, "value", value);
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "OCCUPIED" -> "사용 중";
            case "AVAILABLE" -> "비어있음";
            case "OBJECT_ONLY" -> "물품 감지";
            case "VACANT_LONG" -> "장시간 비움";
            case "SENSOR_DELAY" -> "센서 지연";
            default -> status;
        };
    }

    private void seedSeats() {
        String[] rows = {"A", "B", "C", "D", "E"};
        for (String row : rows) {
            for (int number = 1; number <= 8; number++) {
                String seatId = "3" + row + "-0" + number;
                String status = number % 3 == 0 ? "AVAILABLE" : "OCCUPIED";
                boolean abnormal = false;
                String issueType = "정상 이용";
                int durationMinutes = number * 4;
                String sensorHint = "pressure 0.81 / person 1 / object 0";
                double pressureValue = 0.81;
                boolean personDetected = true;
                boolean objectDetected = false;
                double cameraConfidence = 0.94;
                String gateway = "edge-rpi-01";

                if (List.of("3A-03", "3C-02", "3D-07").contains(seatId)) {
                    status = "OBJECT_ONLY";
                    abnormal = true;
                    issueType = "압력 미감지 상태에서 물품만 감지됨";
                    durationMinutes = 18;
                    sensorHint = "pressure 0.04 / person 0 / object 1";
                    pressureValue = 0.04;
                    personDetected = false;
                    objectDetected = true;
                    cameraConfidence = 0.87;
                    gateway = "edge-rpi-03";
                }
                if (List.of("3A-07", "3D-04").contains(seatId)) {
                    status = "VACANT_LONG";
                    abnormal = true;
                    issueType = "사용 종료 후 장시간 자리 비움";
                    durationMinutes = 22;
                    sensorHint = "pressure 0.00 / person 0 / object 0";
                    pressureValue = 0.00;
                    personDetected = false;
                    objectDetected = false;
                    cameraConfidence = 0.79;
                    gateway = "edge-rpi-02";
                }
                if (List.of("3B-05", "3E-06").contains(seatId)) {
                    status = "SENSOR_DELAY";
                    abnormal = true;
                    issueType = "센서 응답 지연 또는 게이트웨이 재전송 발생";
                    durationMinutes = 11;
                    sensorHint = "latency 1450ms / retry 3";
                    pressureValue = 0.23;
                    personDetected = false;
                    objectDetected = false;
                    cameraConfidence = 0.55;
                    gateway = "edge-rpi-04";
                }

                seats.add(mapOf(
                        "seatId", seatId,
                        "status", status,
                        "statusLabel", statusLabel(status),
                        "lastUpdated", "2026.03.27 17:" + String.format("%02d", (number * 5) % 60),
                        "notes", abnormal ? "관리자 확인 필요" : "정상 이용 중",
                        "abnormal", abnormal,
                        "issueType", issueType,
                        "detectedAt", "2026.03.27 17:" + String.format("%02d", (number * 3) % 60),
                        "durationMinutes", durationMinutes,
                        "sensorHint", sensorHint,
                        "actionStatus", abnormal ? "대기" : "정상",
                        "pressureValue", pressureValue,
                        "personDetected", personDetected,
                        "objectDetected", objectDetected,
                        "cameraConfidence", cameraConfidence,
                        "gateway", gateway
                ));
            }
        }
    }

    private void seedAlertHistory() {
        alertHistory.add(mapOf("id", "AL-1008", "seatId", "3A-07", "studentIdMasked", "2023****", "messageType", "경고 전송", "channel", "앱 푸시", "createdAt", "2026.03.27 17:18", "status", "전송 완료", "message", "장시간 자리 비움으로 경고 메시지를 전송했습니다."));
        alertHistory.add(mapOf("id", "AL-1007", "seatId", "3C-02", "studentIdMasked", "2024****", "messageType", "경고 전송", "channel", "앱 푸시", "createdAt", "2026.03.27 17:10", "status", "전송 완료", "message", "물품 방치 의심 좌석에 대한 경고를 보냈습니다."));
        alertHistory.add(mapOf("id", "AL-1006", "seatId", "3B-05", "studentIdMasked", "2022****", "messageType", "센서 점검 안내", "channel", "관리자 처리", "createdAt", "2026.03.27 16:53", "status", "전송 완료", "message", "센서 지연 이슈로 점검 안내를 등록했습니다."));
        alertHistory.add(mapOf("id", "AL-1005", "seatId", "3D-04", "studentIdMasked", "2021****", "messageType", "경고 전송", "channel", "앱 푸시", "createdAt", "2026.03.27 16:35", "status", "전송 완료", "message", "장시간 비움 좌석으로 분류되어 알림을 보냈습니다."));
        alertHistory.add(mapOf("id", "AL-1004", "seatId", "3D-07", "studentIdMasked", "2023****", "messageType", "처리 완료", "channel", "관리자 처리", "createdAt", "2026.03.27 15:51", "status", "전송 완료", "message", "현장 확인 후 처리 완료로 변경했습니다."));
    }

    private void seedAlertRules() {
        alertRules.add(mapOf("ruleId", "RULE-01", "name", "물품 장기 방치 알림", "condition", "압력 미감지 + 물체 감지", "thresholdMinutes", 10, "channel", "앱 푸시", "enabled", true, "targetType", "물품 방치"));
        alertRules.add(mapOf("ruleId", "RULE-02", "name", "장시간 비움 알림", "condition", "압력 0 + 사람 미감지 지속", "thresholdMinutes", 15, "channel", "앱 푸시", "enabled", true, "targetType", "장시간 비움"));
        alertRules.add(mapOf("ruleId", "RULE-03", "name", "센서 지연 점검", "condition", "게이트웨이 지연 1000ms 초과", "thresholdMinutes", 5, "channel", "관리자 처리", "enabled", true, "targetType", "센서 지연"));
        alertRules.add(mapOf("ruleId", "RULE-04", "name", "심야 자동 알림", "condition", "폐관 전 미반납 좌석", "thresholdMinutes", 5, "channel", "앱 푸시", "enabled", false, "targetType", "폐관 관리"));
        alertRules.add(mapOf("ruleId", "RULE-05", "name", "수동 검토 큐", "condition", "재인식 실패 2회 이상", "thresholdMinutes", 3, "channel", "관리자 처리", "enabled", true, "targetType", "재인식 실패"));
    }

    private void seedLostItems() {
        lostItems.add(mapOf("itemId", "LOST-101", "category", "전자기기", "foundAt", "2026.03.27 16:20", "zone", "3구역", "seatId", "3A-07", "description", "검정색 무선 이어폰 케이스", "status", "보관 중", "custodian", "관리자 김도윤"));
        lostItems.add(mapOf("itemId", "LOST-102", "category", "문구", "foundAt", "2026.03.27 15:40", "zone", "3구역", "seatId", "3C-02", "description", "은색 샤프펜슬", "status", "보관 중", "custodian", "관리자 김도윤"));
        lostItems.add(mapOf("itemId", "LOST-103", "category", "생활용품", "foundAt", "2026.03.27 14:30", "zone", "1구역", "seatId", "1B-12", "description", "파란색 텀블러", "status", "인계 완료", "custodian", "관리자 이나연"));
        lostItems.add(mapOf("itemId", "LOST-104", "category", "전자기기", "foundAt", "2026.03.26 20:10", "zone", "2구역", "seatId", "2D-05", "description", "충전 케이블", "status", "보관 중", "custodian", "관리자 이나연"));
    }

    private void seedDevices() {
        devices.add(mapOf("deviceId", "edge-rpi-01", "type", "Gateway", "zone", "1구역", "status", "정상", "lastSeen", "17:22", "latencyMs", 38, "notes", "좌석 센서 1~12"));
        devices.add(mapOf("deviceId", "edge-rpi-02", "type", "Gateway", "zone", "2구역", "status", "정상", "lastSeen", "17:22", "latencyMs", 42, "notes", "좌석 센서 13~24"));
        devices.add(mapOf("deviceId", "edge-rpi-03", "type", "Gateway", "zone", "3구역", "status", "정상", "lastSeen", "17:22", "latencyMs", 55, "notes", "좌석 센서 25~40"));
        devices.add(mapOf("deviceId", "cam-01", "type", "Camera", "zone", "1구역", "status", "정상", "lastSeen", "17:22", "latencyMs", 64, "notes", "천장형 카메라"));
        devices.add(mapOf("deviceId", "cam-02", "type", "Camera", "zone", "2구역", "status", "정상", "lastSeen", "17:22", "latencyMs", 70, "notes", "천장형 카메라"));
        devices.add(mapOf("deviceId", "cam-03", "type", "Camera", "zone", "3구역", "status", "지연", "lastSeen", "17:20", "latencyMs", 1420, "notes", "프레임 재전송 발생"));
    }

    private void seedSensorLogs() {
        sensorLogs.add(mapOf("id", "LOG-101", "timestamp", "2026.03.27 17:21", "deviceId", "edge-rpi-03", "seatId", "3A-07", "eventType", "VACANT_LONG", "value", "0.00", "message", "장시간 비움 상태 지속", "status", "지연"));
        sensorLogs.add(mapOf("id", "LOG-102", "timestamp", "2026.03.27 17:19", "deviceId", "edge-rpi-03", "seatId", "3C-02", "eventType", "OBJECT_ONLY", "value", "0.04", "message", "물품 방치 의심 상태 재감지", "status", "정상"));
        sensorLogs.add(mapOf("id", "LOG-103", "timestamp", "2026.03.27 17:17", "deviceId", "cam-03", "seatId", "3B-05", "eventType", "FRAME_DELAY", "value", "1450ms", "message", "카메라 프레임 수신 지연", "status", "지연"));
        sensorLogs.add(mapOf("id", "LOG-104", "timestamp", "2026.03.27 17:15", "deviceId", "edge-rpi-02", "seatId", "2C-11", "eventType", "OCCUPIED", "value", "0.82", "message", "정상 착석 감지", "status", "정상"));
        sensorLogs.add(mapOf("id", "LOG-105", "timestamp", "2026.03.27 17:12", "deviceId", "edge-rpi-01", "seatId", "1A-04", "eventType", "AVAILABLE", "value", "0.01", "message", "좌석 반납 완료", "status", "정상"));
    }

    private void seedSettings() {
        settings.put("pushAlertsEnabled", true);
        settings.put("smsAlertsEnabled", false);
        settings.put("quietHoursStart", "22:00");
        settings.put("quietHoursEnd", "07:00");
        settings.put("autoReleaseEnabled", true);
        settings.put("lostItemAutoRegisterEnabled", true);
        settings.put("vacantSeatThresholdMinutes", 15);
        settings.put("objectDetectionThresholdMinutes", 10);
        settings.put("dashboardRefreshSeconds", 8);
        settings.put("sensorDelayThresholdSeconds", 5);
        settings.put("libraryMode", "NORMAL");
    }

    private Map<String, Object> mapOf(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index < pairs.length; index += 2) {
            map.put(String.valueOf(pairs[index]), pairs[index + 1]);
        }
        return map;
    }

    private Map<String, Object> message(String value) {
        return mapOf("message", value);
    }
}
