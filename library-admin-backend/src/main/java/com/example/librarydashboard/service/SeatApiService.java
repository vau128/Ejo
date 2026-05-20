package com.example.librarydashboard.service;

import com.example.librarydashboard.dto.seat.AlertResponse;
import com.example.librarydashboard.dto.seat.CheckInStatusResponse;
import com.example.librarydashboard.dto.seat.LostItemResponse;
import com.example.librarydashboard.dto.seat.LostItemSaveRequest;
import com.example.librarydashboard.dto.seat.LostItemScanTriggerRequest;
import com.example.librarydashboard.dto.seat.MessageResponse;
import com.example.librarydashboard.dto.seat.PostureUpdateRequest;
import com.example.librarydashboard.dto.seat.SeatSquattingRequest;
import com.example.librarydashboard.dto.seat.SeatStatusUpdateRequest;
import com.example.librarydashboard.dto.seat.SeatSummaryResponse;
import com.example.librarydashboard.dto.seat.SquattingThresholdRequest;
import com.example.librarydashboard.dto.seat.SquattingThresholdResponse;
import com.example.librarydashboard.dto.seat.SquattingThresholdUpdateResponse;
import com.example.librarydashboard.entity.LostItem;
import com.example.librarydashboard.entity.PostureLog;
import com.example.librarydashboard.entity.Seat;
import com.example.librarydashboard.entity.SystemSetting;
import com.example.librarydashboard.entity.Warning;
import com.example.librarydashboard.port.out.DashboardOperationsStore;
import com.example.librarydashboard.port.out.DeviceEventGateway;
import com.example.librarydashboard.repository.LostItemRepository;
import com.example.librarydashboard.repository.PostureLogRepository;
import com.example.librarydashboard.repository.SeatRepository;
import com.example.librarydashboard.repository.SystemSettingRepository;
import com.example.librarydashboard.repository.WarningRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Transactional
public class SeatApiService {

    private static final String SQUATTING_THRESHOLD_KEY = "squatting_threshold_minutes";
    private static final int DEFAULT_SQUATTING_THRESHOLD_MINUTES = 60;
    private static final DateTimeFormatter DASHBOARD_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm", Locale.KOREAN);

    private final SeatRepository seatRepository;
    private final WarningRepository warningRepository;
    private final LostItemRepository lostItemRepository;
    private final PostureLogRepository postureLogRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final DashboardOperationsStore dashboardOperationsStore;
    private final DeviceEventGateway deviceEventGateway;
    private final CheckInStatusService checkInStatusService;

    public SeatApiService(
            SeatRepository seatRepository,
            WarningRepository warningRepository,
            LostItemRepository lostItemRepository,
            PostureLogRepository postureLogRepository,
            SystemSettingRepository systemSettingRepository,
            DashboardOperationsStore dashboardOperationsStore,
            DeviceEventGateway deviceEventGateway,
            CheckInStatusService checkInStatusService
    ) {
        this.seatRepository = seatRepository;
        this.warningRepository = warningRepository;
        this.lostItemRepository = lostItemRepository;
        this.postureLogRepository = postureLogRepository;
        this.systemSettingRepository = systemSettingRepository;
        this.dashboardOperationsStore = dashboardOperationsStore;
        this.deviceEventGateway = deviceEventGateway;
        this.checkInStatusService = checkInStatusService;
    }

    public MessageResponse updateSeatStatus(SeatStatusUpdateRequest request) {
        Seat seat = ensureSeat(request.seatNum());
        boolean occupied = request.pressure() != null && request.pressure() > 0;

        seat.setPressure(request.pressure());
        seat.setOccupied(occupied);
        seat.setStatus(occupied ? "OCCUPIED" : baseStatusFor(seat.isCheckedIn(), occupied));
        seat.setUpdatedAt(request.timestamp());
        seatRepository.save(seat);

        syncSeatToDashboard(seat);
        return new MessageResponse("seat status updated");
    }

    public MessageResponse updatePosture(PostureUpdateRequest request) {
        Seat seat = ensureSeat(request.seatNum());
        LocalDateTime sensorTimestamp = toLocalDateTime(request.timestamp());
        LocalDateTime serverTimestamp = LocalDateTime.now();
        int combinedPressure = request.leftPressure() + request.rightPressure() + request.backPressure();
        boolean occupied = request.leftPressure() > 0 || request.rightPressure() > 0 || request.backPressure() > 0;

        seat.setLeftPressure(request.leftPressure());
        seat.setRightPressure(request.rightPressure());
        seat.setBackPressure(request.backPressure());
        seat.setPressure(combinedPressure);
        seat.setPosture(request.posture());
        seat.setPostureTimestamp(sensorTimestamp);
        seat.setOccupied(occupied);
        if (!"SQUATTING".equals(seat.getStatus()) && !"ABNORMAL".equals(seat.getStatus())) {
            seat.setStatus(baseStatusFor(seat.isCheckedIn(), occupied));
        }
        seat.setUpdatedAt(serverTimestamp);
        seatRepository.save(seat);

        postureLogRepository.save(new PostureLog(
                request.seatNum(),
                request.posture(),
                request.leftPressure(),
                request.rightPressure(),
                request.backPressure(),
                sensorTimestamp
        ));

        syncSeatToDashboard(seat);
        return new MessageResponse("posture updated");
    }

    public MessageResponse markSquatting(SeatSquattingRequest request) {
        Seat seat = ensureSeat(request.seatNum());
        String status = normalizeSquattingStatus(request.status());
        LocalDateTime now = LocalDateTime.now();

        seat.setCheckedIn(true);
        seat.setOccupied(false);
        seat.setStatus(status);
        seat.setUpdatedAt(now);
        seatRepository.save(seat);

        Warning warning = warningRepository.save(new Warning(
                seat,
                seat.getSeatNum(),
                "SQUATTING",
                request.status().toLowerCase(Locale.ROOT),
                "Seat " + request.seatNum() + " marked as " + status,
                now
        ));

        syncSeatToDashboard(seat);
        syncWarningToDashboard(warning);
        return new MessageResponse("squatting updated");
    }

    public MessageResponse saveLostItem(LostItemSaveRequest request) {
        Seat seat = ensureSeat(request.seatNum());
        LostItem saved = lostItemRepository.save(new LostItem(
                seat,
                request.seatNum(),
                request.category(),
                request.imageUrl(),
                "FOUND",
                LocalDateTime.now()
        ));

        syncLostItemToDashboard(saved);
        return new MessageResponse("lost item saved");
    }

    public MessageResponse triggerLostItemScan(LostItemScanTriggerRequest request) {
        deviceEventGateway.publishCommand("admin/trigger_lost_item", Map.of(
                "command", request.command(),
                "triggeredAt", LocalDateTime.now().toString()
        ));
        prependSensorLog("LOST_ITEM_SCAN", "ADMIN", "dashboard-web", request.command(), "분실물 스캔 명령을 전송했습니다.", "정상");
        return new MessageResponse("lost item scan triggered");
    }

    @Transactional(readOnly = true)
    public CheckInStatusResponse getCheckInStatus(int seatNum) {
        return new CheckInStatusResponse(seatNum, checkInStatusService.isCheckedIn(seatNum));
    }

    @Transactional(readOnly = true)
    public List<SeatSummaryResponse> getSeats() {
        bootstrapSeatsIfEmpty();
        return seatRepository.findAll().stream()
                .filter(seat -> seat.getSeatNum() != null && seat.getSeatNum() >= 1 && seat.getSeatNum() <= 4)
                .sorted((left, right) -> Integer.compare(left.getSeatNum(), right.getSeatNum()))
                .map(seat -> new SeatSummaryResponse(
                        seat.getSeatNum(),
                        seat.getLocation(),
                        seat.getStatus(),
                        seat.isCheckedIn(),
                        defaultPosture(seat.getPosture()),
                        valueOrZero(seat.getLeftPressure()),
                        valueOrZero(seat.getRightPressure()),
                        valueOrZero(seat.getBackPressure()),
                        seat.getUpdatedAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LostItemResponse> getLostItems() {
        return lostItemRepository.findAllByOrderByDetectedTimeDesc().stream()
                .map(item -> new LostItemResponse(
                        item.getId(),
                        item.getSeatNum(),
                        item.getCategory(),
                        item.getImageUrl(),
                        item.getDetectedTime(),
                        item.getStatus()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> getWarnings() {
        return warningRepository.findAllByOrderByWarningTimeDesc().stream()
                .map(warning -> new AlertResponse(
                        warning.getId(),
                        warning.getSeatNum(),
                        warning.getStatus(),
                        warning.getWarningTime()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public SquattingThresholdResponse getSquattingThreshold() {
        return new SquattingThresholdResponse(getSquattingThresholdMinutes());
    }

    public SquattingThresholdUpdateResponse updateSquattingThreshold(SquattingThresholdRequest request) {
        int thresholdMinutes = normalizeThreshold(request.thresholdMinutes());
        SystemSetting setting = systemSettingRepository.findById(SQUATTING_THRESHOLD_KEY)
                .orElseGet(() -> new SystemSetting(SQUATTING_THRESHOLD_KEY, String.valueOf(DEFAULT_SQUATTING_THRESHOLD_MINUTES)));
        setting.setSettingValue(String.valueOf(thresholdMinutes));
        systemSettingRepository.save(setting);

        Map<String, Object> settings = dashboardOperationsStore.getSettings();
        settings.put("squattingThresholdMinutes", thresholdMinutes);
        dashboardOperationsStore.saveSettings(settings);
        deviceEventGateway.publishCommand("admin/config/squatting_time", Map.of(
                "limit_minutes", thresholdMinutes,
                "triggeredAt", LocalDateTime.now().toString()
        ));
        return new SquattingThresholdUpdateResponse("squatting threshold updated", thresholdMinutes);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDatabaseTest() {
        return Map.of(
                "message", "database connected",
                "seatCount", seatRepository.count()
        );
    }

    public Seat ensureSeat(int seatNum) {
        bootstrapSeatsIfEmpty();
        return seatRepository.findBySeatNum(seatNum)
                .orElseGet(() -> seatRepository.save(new Seat(
                        seatNum,
                        seatLocation(seatNum),
                        seatCode(seatNum),
                        0,
                        "AVAILABLE",
                        false,
                        false,
                        "정상",
                        0,
                        0,
                        0,
                        null,
                        LocalDateTime.now()
                )));
    }

    public int getSquattingThresholdMinutes() {
        return systemSettingRepository.findById(SQUATTING_THRESHOLD_KEY)
                .map(SystemSetting::getSettingValue)
                .map(Integer::parseInt)
                .orElse(DEFAULT_SQUATTING_THRESHOLD_MINUTES);
    }

    public void bootstrapSeatsIfEmpty() {
        if (seatRepository.count() > 0) {
            ensureDefaultThreshold();
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        for (int seatNum = 1; seatNum <= 4; seatNum++) {
            seatRepository.save(new Seat(
                    seatNum,
                    "A-" + seatNum,
                    seatCode(seatNum),
                    0,
                    "AVAILABLE",
                    false,
                    false,
                    "정상",
                    0,
                    0,
                    0,
                    now,
                    now
            ));
        }
        ensureDefaultThreshold();
    }

    private void ensureDefaultThreshold() {
        systemSettingRepository.findById(SQUATTING_THRESHOLD_KEY)
                .orElseGet(() -> systemSettingRepository.save(new SystemSetting(
                        SQUATTING_THRESHOLD_KEY,
                        String.valueOf(DEFAULT_SQUATTING_THRESHOLD_MINUTES)
                )));
    }

    private void syncSeatToDashboard(Seat seat) {
        Map<String, Object> target = dashboardOperationsStore.findSeatById(seat.getSeatCode())
                .orElseGet(() -> createSeatShell(seat.getSeatNum()));

        boolean abnormal = isAbnormalStatus(seat.getStatus());
        target.put("seatId", seat.getSeatCode());
        target.put("seatNumber", seat.getSeatNum());
        target.put("status", seat.getStatus());
        target.put("statusLabel", statusLabel(seat.getStatus()));
        target.put("lastUpdated", formatDashboardTime(seat.getUpdatedAt()));
        target.put("detectedAt", formatDashboardTime(seat.getUpdatedAt()));
        target.put("abnormal", abnormal);
        target.put("issueType", abnormal ? "발권 상태에서 사람 미감지" : "정상 이용");
        target.put("notes", abnormal ? "사석화 기준 시간 이상 지속" : "정상 이용 중");
        target.put("actionStatus", abnormal ? "대기" : "정상");
        target.put("pressureValue", seat.getPressure() == null ? 0.0d : seat.getPressure().doubleValue());
        target.put("personDetected", seat.isOccupied());
        target.put("objectDetected", false);
        target.put("cameraConfidence", seat.isOccupied() ? 0.95d : 0.82d);
        target.put("gateway", "edge-ec2");
        target.put("durationMinutes", abnormal ? getSquattingThresholdMinutes() : 0);
        target.put("sensorHint", sensorHint(seat));
        target.put("checkedIn", seat.isCheckedIn());
        target.put("posture", defaultPosture(seat.getPosture()));
        target.put("leftPressure", valueOrZero(seat.getLeftPressure()));
        target.put("rightPressure", valueOrZero(seat.getRightPressure()));
        target.put("backPressure", valueOrZero(seat.getBackPressure()));
        dashboardOperationsStore.saveSeat(target);
    }

    private void syncWarningToDashboard(Warning warning) {
        Map<String, Object> history = new LinkedHashMap<>();
        history.put("id", "ALERT-" + warning.getId());
        history.put("seatId", seatCode(warning.getSeatNum()));
        history.put("studentIdMasked", "TEMP");
        history.put("messageType", "사석화 감지");
        history.put("channel", "IoT");
        history.put("createdAt", formatDashboardTime(warning.getWarningTime()));
        history.put("status", "전송 완료");
        history.put("message", warning.getMessage());
        dashboardOperationsStore.prependAlertHistory(history);
    }

    private void syncLostItemToDashboard(LostItem item) {
        Map<String, Object> lostItem = new LinkedHashMap<>();
        lostItem.put("itemId", item.getId());
        lostItem.put("category", item.getCategory());
        lostItem.put("foundAt", formatDashboardTime(item.getDetectedTime()));
        lostItem.put("zone", "IoT 구역");
        lostItem.put("seatId", seatCode(item.getSeatNum()));
        lostItem.put("seatNum", item.getSeatNum());
        lostItem.put("description", item.getCategory());
        lostItem.put("status", item.getStatus());
        lostItem.put("custodian", "IoT 자동 등록");
        lostItem.put("imageUrl", item.getImageUrl());
        dashboardOperationsStore.saveLostItem(lostItem);
    }

    private Map<String, Object> createSeatShell(int seatNum) {
        Map<String, Object> shell = new LinkedHashMap<>();
        shell.put("seatId", seatCode(seatNum));
        shell.put("seatNumber", seatNum);
        shell.put("status", "AVAILABLE");
        shell.put("statusLabel", statusLabel("AVAILABLE"));
        shell.put("lastUpdated", formatDashboardTime(LocalDateTime.now()));
        shell.put("notes", "IoT 자동 생성 좌석");
        shell.put("abnormal", false);
        shell.put("issueType", "정상 이용");
        shell.put("detectedAt", formatDashboardTime(LocalDateTime.now()));
        shell.put("durationMinutes", 0);
        shell.put("sensorHint", "left 0 / right 0 / back 0");
        shell.put("actionStatus", "정상");
        shell.put("pressureValue", 0.0d);
        shell.put("personDetected", false);
        shell.put("objectDetected", false);
        shell.put("cameraConfidence", 0.0d);
        shell.put("gateway", "edge-ec2");
        shell.put("checkedIn", false);
        shell.put("posture", "정상");
        shell.put("leftPressure", 0);
        shell.put("rightPressure", 0);
        shell.put("backPressure", 0);
        return shell;
    }

    private void prependSensorLog(String eventType, String seatId, String deviceId, String value, String message, String status) {
        Map<String, Object> log = new LinkedHashMap<>();
        log.put("id", "LOG-" + System.currentTimeMillis());
        log.put("timestamp", formatDashboardTime(LocalDateTime.now()));
        log.put("deviceId", deviceId);
        log.put("seatId", seatId);
        log.put("eventType", eventType);
        log.put("value", value);
        log.put("message", message);
        log.put("status", status);
        dashboardOperationsStore.prependSensorLog(log);
    }

    private String normalizeSquattingStatus(String status) {
        if ("abnormal".equalsIgnoreCase(status)) {
            return "ABNORMAL";
        }
        return "SQUATTING";
    }

    private boolean isAbnormalStatus(String status) {
        return "SQUATTING".equals(status) || "ABNORMAL".equals(status);
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "OCCUPIED" -> "사용 중";
            case "AVAILABLE", "EMPTY" -> "비어있음";
            case "SQUATTING", "ABNORMAL" -> "사석화";
            default -> status;
        };
    }

    private String baseStatusFor(boolean checkedIn, boolean occupied) {
        if (checkedIn && !occupied) {
            return "AVAILABLE";
        }
        return occupied ? "OCCUPIED" : "AVAILABLE";
    }

    private int normalizeThreshold(int thresholdMinutes) {
        return switch (thresholdMinutes) {
            case 30, 60, 120, 240 -> thresholdMinutes;
            default -> DEFAULT_SQUATTING_THRESHOLD_MINUTES;
        };
    }

    private String seatCode(int seatNum) {
        return "seat-" + seatNum;
    }

    private String seatLocation(int seatNum) {
        return "A-" + seatNum;
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
        return posture == null || posture.isBlank() ? "정상" : posture;
    }

    private String formatDashboardTime(LocalDateTime time) {
        return DASHBOARD_TIME_FORMATTER.format(time == null ? LocalDateTime.now() : time);
    }

    private LocalDateTime toLocalDateTime(OffsetDateTime offsetDateTime) {
        return offsetDateTime.toLocalDateTime();
    }
}
