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
        seedSensorLogs();
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
        seats.add(seat("seat-1", 1, "AVAILABLE", false, "정상 이용", 0, "pressure 0.00 / person 0 / object 0", 0.00, false, false, 0.75, "edge-rpi-01", "2026.03.27 17:05"));
        seats.add(seat("seat-2", 2, "OCCUPIED", false, "정상 이용", 12, "pressure 0.82 / person 1 / object 0", 0.82, true, false, 0.94, "edge-rpi-01", "2026.03.27 17:10"));
        seats.add(seat("seat-3", 3, "OBJECT_ONLY", true, "압력 미감지 상태에서 물품만 감지됨", 18, "pressure 0.04 / person 0 / object 1", 0.04, false, true, 0.87, "edge-rpi-02", "2026.03.27 17:15"));
        seats.add(seat("seat-4", 4, "RESERVED", true, "사용 종료 후 장시간 자리 비움", 22, "pressure 0.00 / person 0 / object 0", 0.00, false, false, 0.79, "edge-rpi-02", "2026.03.27 17:20"));
        seats.add(seat("seat-5", 5, "AVAILABLE", false, "정상 이용", 0, "pressure 0.00 / person 0 / object 0", 0.00, false, false, 0.74, "edge-rpi-03", "2026.03.27 17:25"));
        seats.add(seat("seat-6", 6, "OCCUPIED", false, "정상 이용", 7, "pressure 0.78 / person 1 / object 0", 0.78, true, false, 0.90, "edge-rpi-03", "2026.03.27 17:30"));
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
            String lastUpdated
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
                "gateway", gateway
        );
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

    private String statusLabel(String status) {
        return switch (status) {
            case "OCCUPIED" -> "사용 중";
            case "AVAILABLE" -> "비어있음";
            case "OBJECT_ONLY" -> "물품 감지";
            case "VACANT_LONG", "RESERVED" -> "사유석 의심";
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
