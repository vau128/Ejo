package com.example.librarydashboard.service;

import com.example.librarydashboard.dto.IotSeatStatusRequest;
import com.example.librarydashboard.dto.SettingsUpdateRequest;
import com.example.librarydashboard.entity.PostureLog;
import com.example.librarydashboard.entity.Seat;
import com.example.librarydashboard.entity.Warning;
import com.example.librarydashboard.port.out.DashboardOperationsStore;
import com.example.librarydashboard.port.out.DeviceEventGateway;
import com.example.librarydashboard.port.out.NotificationGateway;
import com.example.librarydashboard.port.out.ObjectStorageUrlResolver;
import com.example.librarydashboard.port.out.StudentAccountStore;
import com.example.librarydashboard.repository.PostureLogRepository;
import com.example.librarydashboard.repository.WarningRepository;
import com.example.librarydashboard.repository.SeatRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class DashboardService {

    private static final DateTimeFormatter HISTORY_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm", Locale.KOREAN);
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    private final DashboardOperationsStore dashboardOperationsStore;
    private final DeviceEventGateway deviceEventGateway;
    private final NotificationGateway notificationGateway;
    private final ObjectStorageUrlResolver objectStorageUrlResolver;
    private final SeatRepository seatRepository;
    private final PostureLogRepository postureLogRepository;
    private final WarningRepository warningRepository;
    private final StudentAccountStore studentAccountStore;
    private final AppService appService;

    public DashboardService(
            DashboardOperationsStore dashboardOperationsStore,
            DeviceEventGateway deviceEventGateway,
            NotificationGateway notificationGateway,
            ObjectStorageUrlResolver objectStorageUrlResolver,
            SeatRepository seatRepository,
            PostureLogRepository postureLogRepository,
            WarningRepository warningRepository,
            StudentAccountStore studentAccountStore,
            AppService appService
    ) {
        this.dashboardOperationsStore = dashboardOperationsStore;
        this.deviceEventGateway = deviceEventGateway;
        this.notificationGateway = notificationGateway;
        this.objectStorageUrlResolver = objectStorageUrlResolver;
        this.seatRepository = seatRepository;
        this.postureLogRepository = postureLogRepository;
        this.warningRepository = warningRepository;
        this.studentAccountStore = studentAccountStore;
        this.appService = appService;
    }

    public Map<String, Object> getOverview() {
        List<Map<String, Object>> seats = loadDashboardSeats();
        List<Map<String, Object>> alertHistory = dashboardOperationsStore.findAlertHistory();
        List<Map<String, Object>> lostItems = dashboardOperationsStore.findLostItems();
        List<Map<String, Object>> devices = dashboardOperationsStore.findDevices();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("summary", buildSeatSummary(seats));
        response.put("actionQueue", buildActionQueue(seats).stream().limit(4).toList());
        response.put("recentAlertHistory", alertHistory.stream().limit(4).toList());

        Map<String, Object> zonePreview = new LinkedHashMap<>();
        zonePreview.put("totalSeats", seats.size());
        zonePreview.put("reservedSeats", countByStatus(seats, "RESERVED"));
        zonePreview.put("occupiedSeats", countByStatus(seats, "OCCUPIED"));
        zonePreview.put("abnormalSeats", countAbnormalSeats(seats));
        zonePreview.put("seats", seats.stream().limit(16).toList());
        response.put("zonePreview", zonePreview);

        response.put("lostItemsPreview", lostItems.stream().limit(3).toList());

        Map<String, Object> systemPreview = new LinkedHashMap<>();
        systemPreview.put("connectedSensors", buildSystemSummary(devices).get("sensorConnected"));
        systemPreview.put("cameraOnline", buildSystemSummary(devices).get("cameraOnline"));
        systemPreview.put("delayedFeeds", buildSystemSummary(devices).get("dataDelay"));
        systemPreview.put("devices", devices.stream().limit(3).toList());
        response.put("systemPreview", systemPreview);

        return response;
    }

    public Map<String, Object> getActions() {
        List<Map<String, Object>> seats = loadDashboardSeats();
        List<Map<String, Object>> actionQueue = buildActionQueue(seats);
        List<Map<String, Object>> activeCheckIns = buildActiveCheckInRows(seats);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("pendingWarnings", actionQueue.size());
        summary.put("pendingReleases", actionQueue.stream().filter(item -> !"처리 완료".equals(item.get("status"))).count());
        summary.put("resolvedToday", 0);
        summary.put("activeCheckIns", activeCheckIns.size());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("summary", summary);
        response.put("queue", actionQueue);
        response.put("activeCheckIns", activeCheckIns);
        return response;
    }

    public Map<String, Object> sendWarning(String seatId) {
        Map<String, Object> seat = findSeatView(seatId);
        Seat seatEntity = findSeatEntity(seatId);
        LocalDateTime now = nowInKorea();
        Warning warning = warningRepository.save(new Warning(
                seatEntity,
                seatEntity.getSeatNum(),
                "ADMIN_WARNING",
                "ADMIN_WARNING",
                seatEntity.getSeatNum() + "번 좌석에 관리자 경고가 발송되었습니다.",
                now
        ));
        studentAccountStore.findBySelectedSeatId(seatId).ifPresent(student -> {
            Object countValue = student.get("warningCount");
            int nextCount = countValue instanceof Number ? ((Number) countValue).intValue() + 1 : 1;
            student.put("warningCount", nextCount);
            studentAccountStore.save(student);
        });
        addHistory(seatId, "경고 전송", "앱 푸시", "전송 완료", "좌석 상태 이상으로 경고 메시지를 발송했습니다.");
        addSensorLog("ALERT_PUSH", seatId, "edge-rpi-03", "warn", "관리자 경고 메시지 전송", "정상");
        notificationGateway.sendStudentNotification("masked-student", "좌석 경고", warning.getMessage(), mapOf("seatId", seatId));
        return message("경고를 전송했습니다.");
    }

    public Map<String, Object> releaseSeat(String seatId) {
        Seat seat = findSeatEntity(seatId);
        clearSeatAssignment(seat, "관리자가 좌석 상태를 사용 가능으로 복구했습니다.");
        addHistory(seatId, "상태 해제", "관리자 처리", "전송 완료", "관리자가 좌석 상태를 사용 가능으로 복구했습니다.");
        addSensorLog("SEAT_RELEASE", seatId, "edge-rpi-02", "release", "관리자 상태 해제 처리", "정상");
        deviceEventGateway.publishSeatStatusChanged(seatId, seat.getStatus(), mapSeatForDashboard(seat));
        return message("좌석 상태를 해제했습니다.");
    }

    public Map<String, Object> forceCheckout(String seatId) {
        Seat seat = findSeatEntity(seatId);
        Map<String, Object> releasedStudent = clearSeatAssignment(seat, "관리자가 학생을 강제 퇴실 처리했습니다.");
        String studentName = releasedStudent == null ? "미연결 사용자" : String.valueOf(releasedStudent.get("name"));
        addHistory(seatId, "강제 퇴실", "관리자 처리", "전송 완료", studentName + " 학생을 강제 퇴실 처리했습니다.");
        addSensorLog("FORCED_CHECKOUT", seatId, "dashboard-web", studentName, "관리자 강제 퇴실 처리", "정상");
        deviceEventGateway.publishSeatStatusChanged(seatId, seat.getStatus(), mapSeatForDashboard(seat));
        return message("학생 강제 퇴실 처리를 완료했습니다.");
    }

    public Map<String, Object> resolveIssue(String seatId) {
        Seat seat = findSeatEntity(seatId);
        if ("VACANT_LONG".equals(normalizeSeatStatus(seat.getStatus()))) {
            seat.setStatus(seat.isOccupied() ? "OCCUPIED" : seat.isCheckedIn() ? "RESERVED" : "AVAILABLE");
        }
        seat.setUpdatedAt(nowInKorea());
        seatRepository.save(seat);
        addHistory(seatId, "처리 완료", "관리자 처리", "전송 완료", "비정상 상태 확인 후 처리 완료로 변경했습니다.");
        addSensorLog("ISSUE_RESOLVED", seatId, "edge-rpi-01", "resolved", "현장 점검 후 처리 완료", "정상");
        deviceEventGateway.publishSeatStatusChanged(seatId, seat.getStatus(), mapSeatForDashboard(seat));
        return message("처리 완료로 변경했습니다.");
    }

    public Map<String, Object> getAlertHistory() {
        List<Map<String, Object>> alertHistory = dashboardOperationsStore.findAlertHistory();
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
        List<Map<String, Object>> alertRules = dashboardOperationsStore.findAlertRules();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("enabledRules", alertRules.stream().filter(rule -> Boolean.TRUE.equals(rule.get("enabled"))).count());
        summary.put("disabledRules", alertRules.stream().filter(rule -> Boolean.FALSE.equals(rule.get("enabled"))).count());
        summary.put("pushChannels", alertRules.stream().filter(rule -> Objects.equals(rule.get("channel"), "앱 푸시")).count());

        return mapOf(
                "summary", summary,
                "rules", alertRules
        );
    }

    public Map<String, Object> updateAlertRule(String ruleId, boolean enabled) {
        Map<String, Object> rule = dashboardOperationsStore.findAlertRuleById(ruleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "알림 규칙을 찾을 수 없습니다."));
        rule.put("enabled", enabled);
        dashboardOperationsStore.saveAlertRule(rule);
        return message("알림 규칙 상태를 변경했습니다.");
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("peakUsageRate", "0%");
        summary.put("seatTurnover", "0회");
        summary.put("abnormalFrequency", "0건/일");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("summary", summary);
        response.put("hourlyUsage", List.of(
                point("09:00", 0), point("10:00", 0), point("11:00", 0), point("12:00", 0),
                point("13:00", 0), point("14:00", 0), point("15:00", 0), point("16:00", 0),
                point("17:00", 0), point("18:00", 0)
        ));
        response.put("turnoverTrend", List.of(
                point("09:00", 0), point("10:00", 0), point("11:00", 0), point("12:00", 0),
                point("13:00", 0), point("14:00", 0), point("15:00", 0), point("16:00", 0),
                point("17:00", 0), point("18:00", 0)
        ));
        response.put("abnormalBreakdown", List.of(
                mapOf("label", "사석화", "value", 0, "rate", 0),
                mapOf("label", "자세 이상", "value", 0, "rate", 0),
                mapOf("label", "센서 지연", "value", 0, "rate", 0)
        ));
        return response;
    }

    public Map<String, Object> getHealthcareStatistics() {
        List<Map<String, Object>> seats = loadDashboardSeats();
        List<PostureLog> recentLogs = postureLogRepository.findTop40ByOrderByCreatedAtDesc();
        List<PostureLog> orderedLogs = recentLogs.stream()
                .sorted(Comparator.comparing(PostureLog::getCreatedAt))
                .toList();

        long abnormalLogCount = recentLogs.stream()
                .filter(log -> !isNormalPosture(log.getPosture()))
                .count();
        int sampleCount = recentLogs.size();
        int abnormalRate = sampleCount == 0 ? 0 : (int) Math.round((abnormalLogCount * 100.0d) / sampleCount);

        Map<String, Object> summary = mapOf(
                "activeSeatCount", seats.stream().filter(seat -> Objects.equals(seat.get("status"), "OCCUPIED")).count(),
                "postureSampleCount", sampleCount,
                "abnormalPostureRate", abnormalRate,
                "vacantRiskSeats", seats.stream().filter(seat -> Objects.equals(seat.get("status"), "VACANT_LONG")).count()
        );

        List<Map<String, Object>> postureBreakdown = List.of(
                mapOf("label", "정상", "value", recentLogs.stream().filter(log -> isNormalPosture(log.getPosture())).count()),
                mapOf("label", "허리 숙임", "value", recentLogs.stream().filter(log -> isPosture(log, "거북목", "허리 숙임")).count()),
                mapOf("label", "왼쪽 기울어짐", "value", recentLogs.stream().filter(log -> isPosture(log, "왼쪽")).count()),
                mapOf("label", "오른쪽 기울어짐", "value", recentLogs.stream().filter(log -> isPosture(log, "오른쪽")).count())
        );

        List<Map<String, Object>> pressureTrend = buildPressureTrend(orderedLogs);
        List<Map<String, Object>> liveSeatHealth = seats.stream()
                .map(seat -> mapOf(
                        "seatId", seat.get("seatId"),
                        "seatNumber", seat.get("seatNumber"),
                        "posture", seat.get("posture"),
                        "statusLabel", seat.get("statusLabel"),
                        "checkedIn", seat.get("checkedIn"),
                        "pressureTotal", Math.round(toDouble(seat.get("pressureValue"))),
                        "sensorHint", seat.get("sensorHint")
                ))
                .toList();

        return mapOf(
                "summary", summary,
                "postureBreakdown", postureBreakdown,
                "pressureTrend", pressureTrend,
                "liveSeatHealth", liveSeatHealth
        );
    }

    public Map<String, Object> getSettings() {
        Map<String, Object> settings = dashboardOperationsStore.getSettings();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("summary", mapOf(
                "enabledAutomations", countEnabledAutomations(settings),
                "activeChannels", countActiveChannels(settings),
                "refreshSeconds", settings.get("dashboardRefreshSeconds")
        ));
        response.put("settings", settings);
        return response;
    }

    public Map<String, Object> updateSettings(SettingsUpdateRequest request) {
        Map<String, Object> settings = dashboardOperationsStore.getSettings();
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
        dashboardOperationsStore.saveSettings(settings);
        addSensorLog("SETTINGS_UPDATED", "ADMIN", "dashboard-web", request.libraryMode(), "관리자 설정이 업데이트되었습니다.", "정상");
        return message("설정을 저장했습니다.");
    }

    public Map<String, Object> updateSeatStatusFromIot(IotSeatStatusRequest request) {
        Map<String, Object> seat = dashboardOperationsStore.findSeatById(request.seatId())
                .orElseGet(() -> createSeatShell(request.seatId()));

        String normalizedStatus = normalizeIotStatus(request.status());
        boolean abnormal = isAbnormalStatus(normalizedStatus);
        seat.put("seatId", request.seatId());
        seat.put("status", normalizedStatus);
        seat.put("statusLabel", statusLabel(normalizedStatus));
        seat.put("lastUpdated", request.updateTime());
        seat.put("detectedAt", request.updateTime());
        seat.put("abnormal", abnormal);
        seat.put("actionStatus", abnormal ? "대기" : "정상");
        seat.put("notes", request.status());
        seat.put("issueType", request.status());
        if (request.imageUrl() != null && !request.imageUrl().isBlank()) {
            seat.put("imageUrl", request.imageUrl());
        }
        dashboardOperationsStore.saveSeat(seat);

        addSensorLog(
                eventTypeForStatus(normalizedStatus),
                request.seatId(),
                "edge-local",
                request.status(),
                "로컬 IoT 상태 업데이트",
                abnormal ? "지연" : "정상"
        );

        return mapOf(
                "message", "IoT 좌석 상태를 반영했습니다.",
                "seatId", request.seatId(),
                "status", normalizedStatus
        );
    }

    public Map<String, Object> getZoneSeats(String status, String search) {
        List<Map<String, Object>> seats = loadDashboardSeats();
        List<Map<String, Object>> filtered = seats.stream()
                .filter(seat -> status == null || status.isBlank() || Objects.equals(seat.get("status"), status))
                .filter(seat -> search == null || search.isBlank() || String.valueOf(seat.get("seatId")).toLowerCase().contains(search.toLowerCase()))
                .toList();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("zoneName", "좌석 현황");
        summary.put("totalSeats", seats.size());
        summary.put("reservedSeats", countByStatus(seats, "RESERVED"));
        summary.put("occupiedSeats", countByStatus(seats, "OCCUPIED"));
        summary.put("availableSeats", countByStatus(seats, "AVAILABLE"));
        summary.put("abnormalSeats", countAbnormalSeats(seats));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("summary", summary);
        response.put("seats", filtered);
        return response;
    }

    public Map<String, Object> getSeatDetail(String seatId) {
        Map<String, Object> seat = findSeatView(seatId);
        Map<String, Object> sensor = new LinkedHashMap<>();
        sensor.put("pressureValue", seat.get("pressureValue"));
        sensor.put("personDetected", seat.get("personDetected"));
        sensor.put("objectDetected", seat.get("objectDetected"));
        sensor.put("cameraConfidence", seat.get("cameraConfidence"));
        sensor.put("gateway", seat.get("gateway"));

        Map<String, Object> response = new LinkedHashMap<>(seat);
        response.put("sensor", sensor);
        response.put("history", List.of(
                mapOf("time", HISTORY_FORMATTER.format(nowInKorea().minusMinutes(18)), "statusLabel", "정상 착석", "reason", "압력 센서와 카메라에서 사용자 착석 감지"),
                mapOf("time", HISTORY_FORMATTER.format(nowInKorea().minusMinutes(10)), "statusLabel", String.valueOf(seat.get("statusLabel")), "reason", String.valueOf(seat.get("issueType"))),
                mapOf("time", HISTORY_FORMATTER.format(nowInKorea().minusMinutes(3)), "statusLabel", "관리자 확인 대기", "reason", String.valueOf(seat.get("notes")))
        ));
        return response;
    }

    public Map<String, Object> getAbnormalSeats() {
        List<Map<String, Object>> seats = loadDashboardSeats();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("abnormalCount", countAbnormalSeats(seats));
        summary.put("objectOnly", countByStatus(seats, "OBJECT_ONLY"));
        summary.put("vacantLong", countByStatus(seats, "VACANT_LONG"));
        summary.put("sensorDelay", countByStatus(seats, "SENSOR_DELAY"));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("summary", summary);
        response.put("breakdown", List.of(
                mapOf("label", "물품 감지", "value", countByStatus(seats, "OBJECT_ONLY")),
                mapOf("label", "장시간 비움", "value", countByStatus(seats, "VACANT_LONG")),
                mapOf("label", "센서 지연", "value", countByStatus(seats, "SENSOR_DELAY"))
        ));
        response.put("rows", buildAbnormalSeatRows(seats));
        return response;
    }

    public Map<String, Object> getLostItems() {
        List<Map<String, Object>> lostItems = dashboardOperationsStore.findLostItems().stream()
                .map(this::resolveLostItemImageUrl)
                .toList();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("openCount", lostItems.stream().filter(item -> Objects.equals(item.get("status"), "보관 중")).count());
        summary.put("claimedToday", lostItems.stream().filter(item -> Objects.equals(item.get("status"), "인계 완료")).count());
        summary.put("storageCount", lostItems.size());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("summary", summary);
        response.put("items", lostItems);
        return response;
    }

    private Map<String, Object> resolveLostItemImageUrl(Map<String, Object> item) {
        Map<String, Object> resolved = new LinkedHashMap<>(item);
        Object imageUrl = resolved.get("imageUrl");
        if (imageUrl instanceof String value) {
            resolved.put("imageUrl", objectStorageUrlResolver.resolveReadUrl(value));
        }
        return resolved;
    }

    public Map<String, Object> updateLostItemStatus(String itemId, String status) {
        Map<String, Object> item = dashboardOperationsStore.findLostItemById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "분실물을 찾을 수 없습니다."));
        item.put("status", status);
        dashboardOperationsStore.saveLostItem(item);
        return message("분실물 상태를 변경했습니다.");
    }

    public Map<String, Object> getSystemStatus() {
        List<Map<String, Object>> devices = dashboardOperationsStore.findDevices();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("summary", buildSystemSummary(devices));
        response.put("devices", devices);
        return response;
    }

    public Map<String, Object> getSensorLogs() {
        List<Map<String, Object>> sortedLogs = dashboardOperationsStore.findSensorLogs().stream()
                .sorted(Comparator.comparing((Map<String, Object> item) -> String.valueOf(item.get("timestamp"))).reversed())
                .toList();
        return mapOf("logs", sortedLogs);
    }

    private Map<String, Object> buildSystemSummary(List<Map<String, Object>> devices) {
        long sensorConnected = devices.stream().filter(device -> Objects.equals(device.get("status"), "정상")).count();
        long cameraOnline = devices.stream().filter(device -> Objects.equals(device.get("type"), "Camera") && !Objects.equals(device.get("status"), "오프라인")).count();
        long dataDelay = devices.stream().filter(device -> Objects.equals(device.get("status"), "지연")).count();

        return mapOf(
                "sensorConnected", sensorConnected + "/" + devices.size(),
                "cameraOnline", cameraOnline + "/3",
                "dataDelay", dataDelay + "건"
        );
    }

    private Map<String, Object> buildSeatSummary(List<Map<String, Object>> seats) {
        return mapOf(
                "totalSeats", seats.size(),
                "reservedSeats", countByStatus(seats, "RESERVED"),
                "occupiedSeats", countByStatus(seats, "OCCUPIED"),
                "availableSeats", countByStatus(seats, "AVAILABLE"),
                "abnormalSeats", countAbnormalSeats(seats),
                "alertsToday", dashboardOperationsStore.findAlertHistory().size(),
                "openLostItems", dashboardOperationsStore.findLostItems().stream().filter(item -> Objects.equals(item.get("status"), "보관 중")).count()
        );
    }

    private List<Map<String, Object>> buildActionQueue(List<Map<String, Object>> seats) {
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

    private List<Map<String, Object>> buildActiveCheckInRows(List<Map<String, Object>> seats) {
        return seats.stream()
                .filter(seat -> Boolean.TRUE.equals(seat.get("checkedIn")))
                .map(seat -> {
                    String seatId = String.valueOf(seat.get("seatId"));
                    Map<String, Object> student = studentAccountStore.findBySelectedSeatId(seatId).orElse(null);
                    return mapOf(
                            "seatId", seatId,
                            "studentName", student == null ? "미배정" : student.get("name"),
                            "studentIdMasked", student == null ? "-" : appService.maskStudentId(String.valueOf(student.get("studentId"))),
                            "statusLabel", seat.get("statusLabel"),
                            "posture", seat.get("posture"),
                            "lastUpdated", seat.get("lastUpdated")
                    );
                })
                .toList();
    }

    private List<Map<String, Object>> buildAbnormalSeatRows(List<Map<String, Object>> seats) {
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

    private long countByStatus(List<Map<String, Object>> seats, String status) {
        return seats.stream().filter(seat -> Objects.equals(seat.get("status"), status)).count();
    }

    private long countAbnormalSeats(List<Map<String, Object>> seats) {
        return seats.stream().filter(seat -> Boolean.TRUE.equals(seat.get("abnormal"))).count();
    }

    private Map<String, Object> clearSeatAssignment(Seat seat, String releaseMessage) {
        String seatId = seatCode(seat);
        Map<String, Object> student = studentAccountStore.findBySelectedSeatId(seatId).orElse(null);
        if (student != null) {
            student.put("selectedSeatId", null);
            studentAccountStore.save(student);
        }

        seat.setCheckedIn(false);
        seat.setVacantSince(null);
        seat.setStatus(seat.isOccupied() ? "OCCUPIED" : "AVAILABLE");
        seat.setUpdatedAt(nowInKorea());
        seatRepository.save(seat);

        if (student != null) {
            addSensorLog("APP_CHECKOUT", seatId, "dashboard-web", String.valueOf(student.get("id")), releaseMessage, "정상");
        }
        return student;
    }

    private long countEnabledAutomations(Map<String, Object> settings) {
        return List.of(
                settings.get("pushAlertsEnabled"),
                settings.get("smsAlertsEnabled"),
                settings.get("autoReleaseEnabled"),
                settings.get("lostItemAutoRegisterEnabled")
        ).stream().filter(Boolean.TRUE::equals).count();
    }

    private long countActiveChannels(Map<String, Object> settings) {
        return List.of(settings.get("pushAlertsEnabled"), settings.get("smsAlertsEnabled"))
                .stream()
                .filter(Boolean.TRUE::equals)
                .count();
    }

    private String severityForStatus(String status) {
        return switch (status) {
            case "OBJECT_ONLY", "ITEM" -> "주의";
            case "VACANT_LONG" -> "긴급";
            case "SENSOR_DELAY" -> "지연";
            case "RESERVED" -> "안내";
            default -> "확인";
        };
    }

    private List<Map<String, Object>> buildPressureTrend(List<PostureLog> orderedLogs) {
        if (orderedLogs.isEmpty()) {
            return List.of(
                    point("표본 1", 0),
                    point("표본 2", 0),
                    point("표본 3", 0),
                    point("표본 4", 0),
                    point("표본 5", 0),
                    point("표본 6", 0)
            );
        }

        List<PostureLog> recent = orderedLogs.stream()
                .skip(Math.max(0, orderedLogs.size() - 8))
                .toList();
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int index = 0; index < recent.size(); index++) {
            PostureLog log = recent.get(index);
            int pressureTotal = valueOrZero(log.getLeftPressure()) + valueOrZero(log.getRightPressure()) + valueOrZero(log.getBackPressure());
            trend.add(point("표본 " + (index + 1), pressureTotal));
        }
        return trend;
    }

    private boolean isNormalPosture(String posture) {
        return posture == null || posture.isBlank() || "정상".equals(posture) || "바른 자세 유지 중".equals(posture);
    }

    private boolean isPosture(PostureLog log, String... keywords) {
        String posture = log.getPosture();
        if (posture == null) {
            return false;
        }
        for (String keyword : keywords) {
            if (posture.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private double toDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0.0d;
    }

    private Map<String, Object> createSeatShell(String seatId) {
        return new LinkedHashMap<>(mapOf(
                "seatId", seatId,
                "status", "AVAILABLE",
                "statusLabel", statusLabel("AVAILABLE"),
                "lastUpdated", HISTORY_FORMATTER.format(nowInKorea()),
                "notes", "로컬 IoT 좌석",
                "abnormal", false,
                "issueType", "정상 이용",
                "detectedAt", HISTORY_FORMATTER.format(nowInKorea()),
                "durationMinutes", 0,
                "sensorHint", "mqtt local test",
                "actionStatus", "정상",
                "pressureValue", 0.0,
                "personDetected", false,
                "objectDetected", false,
                "cameraConfidence", 0.0,
                "gateway", "edge-local"
        ));
    }

    private String normalizeIotStatus(String status) {
        return switch (status) {
            case "정상 사용중" -> "OCCUPIED";
            case "정상 빈좌석" -> "AVAILABLE";
            case "사석화 의심 (자리비움)", "사석화 확정" -> "VACANT_LONG";
            case "분실물 확인 중", "분실물 확정" -> "OBJECT_ONLY";
            case "사석화 (퇴실후 미퇴거)" -> "RESERVED";
            default -> status;
        };
    }

    private boolean isAbnormalStatus(String status) {
        return switch (status) {
            case "OBJECT_ONLY", "VACANT_LONG", "SENSOR_DELAY" -> true;
            default -> false;
        };
    }

    private String eventTypeForStatus(String status) {
        return switch (status) {
            case "OCCUPIED" -> "OCCUPIED";
            case "AVAILABLE" -> "AVAILABLE";
            case "OBJECT_ONLY" -> "OBJECT_ONLY";
            case "VACANT_LONG" -> "VACANT_LONG";
            case "RESERVED" -> "CHECKOUT_ITEM";
            default -> "IOT_STATUS";
        };
    }

    private Seat findSeatEntity(String seatId) {
        bootstrapSeatsIfEmpty();
        int seatNum = parseSeatNum(seatId);
        return seatRepository.findBySeatNum(seatNum)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "좌석을 찾을 수 없습니다."));
    }

    private Map<String, Object> findSeatView(String seatId) {
        return mapSeatForDashboard(findSeatEntity(seatId));
    }

    private void addHistory(String seatId, String messageType, String channel, String status, String message) {
        int nextSequence = dashboardOperationsStore.findAlertHistory().size() + 1001;
        dashboardOperationsStore.prependAlertHistory(mapOf(
                "id", "AL-" + nextSequence,
                "seatId", seatId,
                "studentIdMasked", "2023****",
                "messageType", messageType,
                "channel", channel,
                "createdAt", HISTORY_FORMATTER.format(nowInKorea()),
                "status", status,
                "message", message
        ));
    }

    private void addSensorLog(String eventType, String seatId, String deviceId, String value, String message, String status) {
        dashboardOperationsStore.prependSensorLog(mapOf(
                "id", UUID.randomUUID().toString(),
                "timestamp", HISTORY_FORMATTER.format(nowInKorea()),
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
            case "RESERVED" -> "발권됨";
            case "OBJECT_ONLY", "ITEM" -> "물품 감지";
            case "VACANT_LONG" -> "장시간 비움";
            case "SENSOR_DELAY" -> "센서 지연";
            default -> status;
        };
    }

    private List<Map<String, Object>> loadDashboardSeats() {
        bootstrapSeatsIfEmpty();
        return seatRepository.findAll().stream()
                .filter(seat -> seat.getSeatNum() != null && seat.getSeatNum() >= 1 && seat.getSeatNum() <= 4)
                .sorted(Comparator.comparing(Seat::getSeatNum))
                .map(this::mapSeatForDashboard)
                .toList();
    }

    private void bootstrapSeatsIfEmpty() {
        if (seatRepository.count() > 0) {
            return;
        }

        LocalDateTime now = nowInKorea();
        for (int seatNum = 1; seatNum <= 4; seatNum++) {
            seatRepository.save(new Seat(
                    seatNum,
                    "A-" + seatNum,
                    "seat-" + seatNum,
                    0,
                    "AVAILABLE",
                    false,
                    false,
                    "정상",
                    0,
                    0,
                    0,
                    now,
                    null,
                    now
            ));
        }
    }

    private Map<String, Object> mapSeatForDashboard(Seat seat) {
        String normalizedStatus = normalizeSeatStatus(seat.getStatus());
        boolean abnormal = isAbnormalStatus(normalizedStatus);
        return mapOf(
                "seatId", seatCode(seat),
                "seatNumber", seat.getSeatNum(),
                "status", normalizedStatus,
                "statusLabel", statusLabel(normalizedStatus),
                "lastUpdated", HISTORY_FORMATTER.format(defaultTime(seat.getUpdatedAt())),
                "notes", notesForStatus(normalizedStatus),
                "abnormal", abnormal,
                "issueType", issueTypeForStatus(normalizedStatus),
                "detectedAt", HISTORY_FORMATTER.format(defaultTime(seat.getUpdatedAt())),
                "durationMinutes", calculateVacantMinutes(seat),
                "sensorHint", sensorHint(seat),
                "actionStatus", abnormal ? "대기" : "정상",
                "pressureValue", seat.getPressure() == null ? 0.0d : seat.getPressure().doubleValue(),
                "personDetected", seat.isOccupied(),
                "objectDetected", "OBJECT_ONLY".equals(normalizedStatus),
                "cameraConfidence", seat.isOccupied() ? 0.95d : 0.82d,
                "gateway", gatewayForSeat(seat),
                "checkedIn", seat.isCheckedIn(),
                "posture", defaultPosture(seat.getPosture()),
                "leftPressure", valueOrZero(seat.getLeftPressure()),
                "rightPressure", valueOrZero(seat.getRightPressure()),
                "backPressure", valueOrZero(seat.getBackPressure())
        );
    }

    private String normalizeSeatStatus(String status) {
        return switch (status) {
            case "SQUATTING", "ABNORMAL" -> "VACANT_LONG";
            default -> status == null || status.isBlank() ? "AVAILABLE" : status;
        };
    }

    private String seatCode(Seat seat) {
        if (seat.getSeatCode() != null && !seat.getSeatCode().isBlank()) {
            return seat.getSeatCode();
        }
        return "seat-" + seat.getSeatNum();
    }

    private String gatewayForSeat(Seat seat) {
        return seat.getSeatNum() != null && seat.getSeatNum() <= 2 ? "edge-rpi-01" : "edge-rpi-02";
    }

    private String sensorHint(Seat seat) {
        return "left " + valueOrZero(seat.getLeftPressure())
                + " / right " + valueOrZero(seat.getRightPressure())
                + " / back " + valueOrZero(seat.getBackPressure());
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private String defaultPosture(String posture) {
        if (posture == null || posture.isBlank()) {
            return "정상";
        }
        if (posture.contains("거북목") || posture.contains("허리") || posture.contains("숙임")) {
            return "허리 숙임";
        }
        return posture;
    }

    private LocalDateTime defaultTime(LocalDateTime time) {
        return time == null ? nowInKorea() : time;
    }

    private LocalDateTime nowInKorea() {
        return LocalDateTime.now(KOREA_ZONE);
    }

    private int calculateVacantMinutes(Seat seat) {
        if (!seat.isCheckedIn() || seat.isOccupied() || seat.getVacantSince() == null) {
            return 0;
        }
        return (int) Math.max(0, ChronoUnit.MINUTES.between(seat.getVacantSince(), defaultTime(seat.getUpdatedAt())));
    }

    private String issueTypeForStatus(String status) {
        return switch (status) {
            case "RESERVED" -> "발권 후 미착석";
            case "VACANT_LONG" -> "장시간 자리 비움";
            case "OBJECT_ONLY" -> "물품만 감지됨";
            case "SENSOR_DELAY" -> "센서 수집 지연";
            default -> "정상 이용";
        };
    }

    private String notesForStatus(String status) {
        return switch (status) {
            case "RESERVED" -> "학생이 좌석을 선택했지만 아직 착석이 감지되지 않았습니다.";
            case "VACANT_LONG" -> "발권 또는 착석 이력 이후 압력 미감지 시간이 기준을 초과했습니다.";
            case "OBJECT_ONLY" -> "사람 없이 물품만 감지된 좌석입니다.";
            case "SENSOR_DELAY" -> "센서 데이터 수집이 지연되고 있습니다.";
            default -> "정상 이용 중";
        };
    }

    private int parseSeatNum(String seatId) {
        String digits = seatId == null ? "" : seatId.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "좌석 번호 형식이 올바르지 않습니다.");
        }
        return Integer.parseInt(digits);
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
