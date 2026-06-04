package com.example.librarydashboard.adapter.out.memory;

import com.example.librarydashboard.port.out.DashboardOperationsStore;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class InMemoryDashboardOperationsStore implements DashboardOperationsStore {

    private final List<Map<String, Object>> seats = new ArrayList<>();
    private final List<Map<String, Object>> alertHistory = new ArrayList<>();
    private final List<Map<String, Object>> alertRules = new ArrayList<>();
    private final List<Map<String, Object>> lostItems = new ArrayList<>();
    private final List<Map<String, Object>> devices = new ArrayList<>();
    private final List<Map<String, Object>> sensorLogs = new ArrayList<>();
    private final Map<String, Object> settings = new LinkedHashMap<>();

    public InMemoryDashboardOperationsStore() {
        seedSeats();
        seedAlertHistory();
        seedAlertRules();
        seedLostItems();
        seedDevices();
        seedSettings();
    }

    @Override
    public List<Map<String, Object>> findAllSeats() {
        return seats.stream().map(item -> (Map<String, Object>) new LinkedHashMap<>(item)).toList();
    }

    @Override
    public Optional<Map<String, Object>> findSeatById(String seatId) {
        return seats.stream().filter(item -> seatId.equals(item.get("seatId"))).findFirst().map(LinkedHashMap::new);
    }

    @Override
    public Map<String, Object> saveSeat(Map<String, Object> seat) {
        return saveById(seats, seat, "seatId");
    }

    @Override
    public List<Map<String, Object>> findAlertHistory() {
        return alertHistory.stream().map(item -> (Map<String, Object>) new LinkedHashMap<>(item)).toList();
    }

    @Override
    public void prependAlertHistory(Map<String, Object> history) {
        alertHistory.add(0, new LinkedHashMap<>(history));
    }

    @Override
    public void clearAlertHistory() {
        alertHistory.clear();
    }

    @Override
    public List<Map<String, Object>> findAlertRules() {
        return alertRules.stream().map(item -> (Map<String, Object>) new LinkedHashMap<>(item)).toList();
    }

    @Override
    public Optional<Map<String, Object>> findAlertRuleById(String ruleId) {
        return alertRules.stream().filter(item -> ruleId.equals(item.get("ruleId"))).findFirst().map(LinkedHashMap::new);
    }

    @Override
    public Map<String, Object> saveAlertRule(Map<String, Object> rule) {
        return saveById(alertRules, rule, "ruleId");
    }

    @Override
    public List<Map<String, Object>> findLostItems() {
        return lostItems.stream().map(item -> (Map<String, Object>) new LinkedHashMap<>(item)).toList();
    }

    @Override
    public Optional<Map<String, Object>> findLostItemById(String itemId) {
        return lostItems.stream().filter(item -> itemId.equals(item.get("itemId"))).findFirst().map(LinkedHashMap::new);
    }

    @Override
    public Map<String, Object> saveLostItem(Map<String, Object> item) {
        return saveById(lostItems, item, "itemId");
    }

    @Override
    public void clearLostItems() {
        lostItems.clear();
    }

    @Override
    public List<Map<String, Object>> findDevices() {
        return devices.stream().map(item -> (Map<String, Object>) new LinkedHashMap<>(item)).toList();
    }

    @Override
    public List<Map<String, Object>> findSensorLogs() {
        return sensorLogs.stream().map(item -> (Map<String, Object>) new LinkedHashMap<>(item)).toList();
    }

    @Override
    public void prependSensorLog(Map<String, Object> log) {
        sensorLogs.add(0, new LinkedHashMap<>(log));
    }

    @Override
    public void clearSensorLogs() {
        sensorLogs.clear();
    }

    @Override
    public Map<String, Object> getSettings() {
        return new LinkedHashMap<>(settings);
    }

    @Override
    public Map<String, Object> saveSettings(Map<String, Object> nextSettings) {
        settings.clear();
        settings.putAll(nextSettings);
        return new LinkedHashMap<>(settings);
    }

    private Map<String, Object> saveById(List<Map<String, Object>> target, Map<String, Object> source, String key) {
        String id = String.valueOf(source.get(key));
        for (int i = 0; i < target.size(); i++) {
            if (id.equals(target.get(i).get(key))) {
                target.set(i, new LinkedHashMap<>(source));
                return new LinkedHashMap<>(target.get(i));
            }
        }
        target.add(new LinkedHashMap<>(source));
        return new LinkedHashMap<>(source);
    }

    private void seedSeats() {
        seats.add(seat("seat-1", 1, "AVAILABLE", false, "정상 이용", 0, "left 0 / right 0 / back 0", 0.00, false, false, 0.75, "edge-rpi-01", "2026.03.27 17:05", false, "정상", 0, 0, 0));
        seats.add(seat("seat-2", 2, "AVAILABLE", false, "정상 이용", 0, "left 0 / right 0 / back 0", 0.00, false, false, 0.75, "edge-rpi-01", "2026.03.27 17:05", false, "정상", 0, 0, 0));
        seats.add(seat("seat-3", 3, "AVAILABLE", false, "정상 이용", 0, "left 0 / right 0 / back 0", 0.00, false, false, 0.75, "edge-rpi-02", "2026.03.27 17:05", false, "정상", 0, 0, 0));
        seats.add(seat("seat-4", 4, "AVAILABLE", false, "정상 이용", 0, "left 0 / right 0 / back 0", 0.00, false, false, 0.75, "edge-rpi-02", "2026.03.27 17:05", false, "정상", 0, 0, 0));
    }

    private Map<String, Object> seat(
            String seatId,
            int seatNumber,
            String status,
            boolean abnormal,
            String issueType,
            int durationMinutes,
            String sensorHint,
            double pressureValue,
            boolean personDetected,
            boolean objectDetected,
            double cameraConfidence,
            String gateway,
            String lastUpdated,
            boolean checkedIn,
            String posture,
            int leftPressure,
            int rightPressure,
            int backPressure
    ) {
        return mapOf(
                "seatId", seatId,
                "seatNumber", seatNumber,
                "status", status,
                "statusLabel", statusLabel(status),
                "lastUpdated", lastUpdated,
                "notes", abnormal ? "관리자 확인 필요" : "정상 이용 중",
                "abnormal", abnormal,
                "issueType", issueType,
                "detectedAt", lastUpdated,
                "durationMinutes", durationMinutes,
                "sensorHint", sensorHint,
                "actionStatus", abnormal ? "대기" : "정상",
                "pressureValue", pressureValue,
                "personDetected", personDetected,
                "objectDetected", objectDetected,
                "cameraConfidence", cameraConfidence,
                "gateway", gateway,
                "checkedIn", checkedIn,
                "posture", posture,
                "leftPressure", leftPressure,
                "rightPressure", rightPressure,
                "backPressure", backPressure
        );
    }

    private void seedAlertHistory() {
    }

    private void seedAlertRules() {
        alertRules.add(mapOf("ruleId", "RULE-02", "name", "장시간 비움 알림", "condition", "압력 0 + 사람 미감지 지속", "thresholdMinutes", 15, "channel", "앱 푸시", "enabled", true, "targetType", "장시간 비움"));
        alertRules.add(mapOf("ruleId", "RULE-03", "name", "센서 지연 점검", "condition", "게이트웨이 지연 1000ms 초과", "thresholdMinutes", 5, "channel", "관리자 처리", "enabled", true, "targetType", "센서 지연"));
        alertRules.add(mapOf("ruleId", "RULE-04", "name", "심야 자동 알림", "condition", "폐관 전 미반납 좌석", "thresholdMinutes", 5, "channel", "앱 푸시", "enabled", false, "targetType", "폐관 관리"));
        alertRules.add(mapOf("ruleId", "RULE-05", "name", "수동 검토 큐", "condition", "재인식 실패 2회 이상", "thresholdMinutes", 3, "channel", "관리자 처리", "enabled", true, "targetType", "재인식 실패"));
    }

    private void seedLostItems() {
    }

    private void seedDevices() {
        devices.add(mapOf("deviceId", "edge-rpi-01", "type", "Gateway", "zone", "1구역", "status", "정상", "lastSeen", "17:22", "latencyMs", 38, "notes", "좌석 센서 1~12"));
        devices.add(mapOf("deviceId", "edge-rpi-02", "type", "Gateway", "zone", "2구역", "status", "정상", "lastSeen", "17:22", "latencyMs", 42, "notes", "좌석 센서 13~24"));
        devices.add(mapOf("deviceId", "edge-rpi-03", "type", "Gateway", "zone", "3구역", "status", "정상", "lastSeen", "17:22", "latencyMs", 55, "notes", "좌석 센서 25~40"));
        devices.add(mapOf("deviceId", "cam-01", "type", "Camera", "zone", "1구역", "status", "정상", "lastSeen", "17:22", "latencyMs", 64, "notes", "천장형 카메라"));
        devices.add(mapOf("deviceId", "cam-02", "type", "Camera", "zone", "2구역", "status", "정상", "lastSeen", "17:22", "latencyMs", 70, "notes", "천장형 카메라"));
        devices.add(mapOf("deviceId", "cam-03", "type", "Camera", "zone", "3구역", "status", "지연", "lastSeen", "17:20", "latencyMs", 1420, "notes", "프레임 재전송 발생"));
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
        settings.put("squattingThresholdMinutes", 60);
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "OCCUPIED" -> "사용 중";
            case "AVAILABLE" -> "비어있음";
            case "SQUATTING" -> "사석화";
            case "ABNORMAL" -> "비정상";
            case "SENSOR_DELAY" -> "센서 지연";
            default -> status;
        };
    }

    private Map<String, Object> mapOf(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index < pairs.length; index += 2) {
            map.put(String.valueOf(pairs[index]), pairs[index + 1]);
        }
        return map;
    }
}
