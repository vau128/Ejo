package com.example.librarydashboard.service;

import com.example.librarydashboard.config.IotMqttProperties;
import com.example.librarydashboard.config.IotProperties;
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
import com.example.librarydashboard.port.out.ObjectStorageUrlResolver;
import com.example.librarydashboard.port.out.StudentAccountStore;
import com.example.librarydashboard.repository.LostItemRepository;
import com.example.librarydashboard.repository.PostureLogRepository;
import com.example.librarydashboard.repository.SeatRepository;
import com.example.librarydashboard.repository.SeatUsageRepository;
import com.example.librarydashboard.repository.SystemSettingRepository;
import com.example.librarydashboard.repository.WarningRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
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
    private static final DateTimeFormatter APP_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.KOREAN);
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final Logger log = LoggerFactory.getLogger(SeatApiService.class);

    private final SeatRepository seatRepository;
    private final WarningRepository warningRepository;
    private final LostItemRepository lostItemRepository;
    private final PostureLogRepository postureLogRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final DashboardOperationsStore dashboardOperationsStore;
    private final DeviceEventGateway deviceEventGateway;
    private final IotAdminTriggerClient iotAdminTriggerClient;
    private final IotProperties iotProperties;
    private final IotMqttProperties iotMqttProperties;
    private final ObjectStorageUrlResolver objectStorageUrlResolver;
    private final CheckInStatusService checkInStatusService;
    private final StudentAccountStore studentAccountStore;
    private final SeatUsageRepository seatUsageRepository;

    public SeatApiService(
            SeatRepository seatRepository,
            WarningRepository warningRepository,
            LostItemRepository lostItemRepository,
            PostureLogRepository postureLogRepository,
            SystemSettingRepository systemSettingRepository,
            DashboardOperationsStore dashboardOperationsStore,
            DeviceEventGateway deviceEventGateway,
            IotAdminTriggerClient iotAdminTriggerClient,
            IotProperties iotProperties,
            IotMqttProperties iotMqttProperties,
            ObjectStorageUrlResolver objectStorageUrlResolver,
            CheckInStatusService checkInStatusService,
            StudentAccountStore studentAccountStore,
            SeatUsageRepository seatUsageRepository
    ) {
        this.seatRepository = seatRepository;
        this.warningRepository = warningRepository;
        this.lostItemRepository = lostItemRepository;
        this.postureLogRepository = postureLogRepository;
        this.systemSettingRepository = systemSettingRepository;
        this.dashboardOperationsStore = dashboardOperationsStore;
        this.deviceEventGateway = deviceEventGateway;
        this.iotAdminTriggerClient = iotAdminTriggerClient;
        this.iotProperties = iotProperties;
        this.iotMqttProperties = iotMqttProperties;
        this.objectStorageUrlResolver = objectStorageUrlResolver;
        this.checkInStatusService = checkInStatusService;
        this.studentAccountStore = studentAccountStore;
        this.seatUsageRepository = seatUsageRepository;
    }

    public MessageResponse updateSeatStatus(SeatStatusUpdateRequest request) {
        Seat seat = ensureSeat(request.seatNum());
        String previousStatus = seat.getStatus();
        boolean occupied = request.pressure() != null && request.pressure() > 0;
        LocalDateTime sensorTimestamp = toLocalDateTime(request.timestamp());

        seat.setPressure(request.pressure());
        updateSeatOccupancyState(seat, occupied, sensorTimestamp);
        seat.setUpdatedAt(sensorTimestamp);
        seatRepository.save(seat);

        syncSeatToDashboard(seat);
        appendSquattingWarningIfNeeded(seat, previousStatus, sensorTimestamp);
        return new MessageResponse("seat status updated");
    }

    public MessageResponse updatePosture(PostureUpdateRequest request) {
        Seat seat = ensureSeat(request.seatNum());
        String previousStatus = seat.getStatus();
        LocalDateTime sensorTimestamp = toLocalDateTime(request.timestamp());
        LocalDateTime serverTimestamp = nowInKorea();
        int combinedPressure = request.leftPressure() + request.rightPressure() + request.backPressure();
        boolean occupied = request.leftPressure() > 0 || request.rightPressure() > 0 || request.backPressure() > 0;

        seat.setLeftPressure(request.leftPressure());
        seat.setRightPressure(request.rightPressure());
        seat.setBackPressure(request.backPressure());
        seat.setPressure(combinedPressure);
        seat.setPosture(request.posture());
        seat.setPostureTimestamp(sensorTimestamp);
        updateSeatOccupancyState(seat, occupied, sensorTimestamp);
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
        appendSquattingWarningIfNeeded(seat, previousStatus, sensorTimestamp);
        return new MessageResponse("posture updated");
    }

    public MessageResponse markSquatting(SeatSquattingRequest request) {
        Seat seat = ensureSeat(request.seatNum());
        String status = normalizeSquattingStatus(request.status());
        LocalDateTime now = nowInKorea();

        seat.setCheckedIn(true);
        seat.setOccupied(false);
        seat.setStatus(status);
        seat.setVacantSince(now);
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

    public MessageResponse resetDemoState() {
        warningRepository.deleteAllInBatch();
        lostItemRepository.deleteAllInBatch();
        postureLogRepository.deleteAllInBatch();
        seatUsageRepository.deleteAllInBatch();

        LocalDateTime now = nowInKorea();
        for (int seatNum = 1; seatNum <= 4; seatNum++) {
            Seat seat = ensureSeat(seatNum);
            seat.setPressure(0);
            seat.setStatus("AVAILABLE");
            seat.setCheckedIn(false);
            seat.setOccupied(false);
            seat.setPosture("정상");
            seat.setLeftPressure(0);
            seat.setRightPressure(0);
            seat.setBackPressure(0);
            seat.setPostureTimestamp(null);
            seat.setVacantSince(null);
            seat.setUpdatedAt(now);
            seatRepository.save(seat);
            syncSeatToDashboard(seat);
        }

        dashboardOperationsStore.clearAlertHistory();
        dashboardOperationsStore.clearLostItems();
        dashboardOperationsStore.clearSensorLogs();
        studentAccountStore.resetForTesting();

        return new MessageResponse("demo state reset");
    }

    public MessageResponse saveLostItem(LostItemSaveRequest request) {
        Seat seat = ensureSeat(request.seatNum());
        LocalDateTime detectedAt = nowInKorea();
        LostItem saved = lostItemRepository.save(new LostItem(
                seat,
                request.seatNum(),
                request.category(),
                request.imageUrl(),
                "FOUND",
                detectedAt
        ));

        syncLostItemToDashboard(saved);
        return new MessageResponse("lost item saved");
    }

    public MessageResponse triggerLostItemScan(LostItemScanTriggerRequest request) {
        if (!iotMqttProperties.enabled()) {
            return triggerLostItemScanOverHttpFallback(request);
        }

        try {
            deviceEventGateway.publishCommand("admin/trigger_lost_item", Map.of(
                    "command", request.command(),
                    "triggeredAt", formatAppTime(nowInKorea())
            ));
            prependSensorLog("LOST_ITEM_SCAN", "ADMIN", "dashboard-web", request.command(), "분실물 스캔 명령을 전송했습니다.", "정상");
            return new MessageResponse("lost item scan triggered");
        } catch (RuntimeException mqttException) {
            log.error("Failed to publish lost item scan command over MQTT", mqttException);
            return triggerLostItemScanOverHttpFallback(request);
        }
    }

    private MessageResponse triggerLostItemScanOverHttpFallback(LostItemScanTriggerRequest request) {
        if (!iotProperties.enabled()) {
            prependSensorLog("LOST_ITEM_SCAN", "ADMIN", "dashboard-web", request.command(), "분실물 스캔 명령 전송에 실패했습니다. IoT 연동이 비활성화되어 있습니다.", "오류");
            return new MessageResponse("분실물 스캔 요청은 접수됐지만 라즈베리파이 전송에 실패했습니다.");
        }

        try {
            iotAdminTriggerClient.triggerLostItemScan(request.command());
            prependSensorLog("LOST_ITEM_SCAN", "ADMIN", "dashboard-web", request.command(), "분실물 스캔 명령을 HTTP fallback으로 전송했습니다.", "정상");
            return new MessageResponse("lost item scan triggered");
        } catch (RuntimeException httpException) {
            log.error("Failed to publish lost item scan command over HTTP fallback", httpException);
            prependSensorLog("LOST_ITEM_SCAN", "ADMIN", "dashboard-web", request.command(), "분실물 스캔 명령 전송에 실패했습니다.", "오류");
            return new MessageResponse("분실물 스캔 요청은 접수됐지만 라즈베리파이 전송에 실패했습니다.");
        }
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
                        objectStorageUrlResolver.resolveReadUrl(item.getImageUrl()),
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
                        warning.getWarningType(),
                        warning.getStatus(),
                        warning.getMessage(),
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
        try {
            deviceEventGateway.publishCommand("admin/config/squatting_time", squattingThresholdPayload(thresholdMinutes));
            return new SquattingThresholdUpdateResponse("squatting threshold updated", thresholdMinutes);
        } catch (RuntimeException exception) {
            log.error("Failed to publish squatting threshold update", exception);
            return new SquattingThresholdUpdateResponse("사석화 기준 저장은 완료됐지만 라즈베리파이 전송에 실패했습니다.", thresholdMinutes);
        }
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
        Seat existing = seatRepository.findBySeatNum(seatNum)
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
                        null,
                        nowInKorea()
                )));
        normalizeSeatMetadata(existing);
        return existing;
    }

    public int getSquattingThresholdMinutes() {
        return systemSettingRepository.findById(SQUATTING_THRESHOLD_KEY)
                .map(SystemSetting::getSettingValue)
                .map(Integer::parseInt)
                .orElse(DEFAULT_SQUATTING_THRESHOLD_MINUTES);
    }

    public void bootstrapSeatsIfEmpty() {
        if (seatRepository.count() > 0) {
            seatRepository.findAll().stream()
                    .filter(seat -> seat.getSeatNum() != null && seat.getSeatNum() >= 1 && seat.getSeatNum() <= 4)
                    .forEach(this::normalizeSeatMetadata);
            ensureDefaultThreshold();
            return;
        }

        LocalDateTime now = nowInKorea();
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
                    null,
                    now
            ));
        }
        ensureDefaultThreshold();
    }

    public void syncSeatToDashboardState(Seat seat) {
        syncSeatToDashboard(seat);
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
        target.put("issueType", dashboardIssueType(seat.getStatus()));
        target.put("notes", dashboardNotes(seat.getStatus()));
        target.put("actionStatus", abnormal ? "대기" : "정상");
        target.put("pressureValue", seat.getPressure() == null ? 0.0d : seat.getPressure().doubleValue());
        target.put("personDetected", seat.isOccupied());
        target.put("objectDetected", false);
        target.put("cameraConfidence", seat.isOccupied() ? 0.95d : 0.82d);
        target.put("gateway", "edge-ec2");
        target.put("durationMinutes", calculateVacantMinutes(seat));
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
        history.put("messageType", dashboardWarningType(warning));
        history.put("channel", "앱 푸시");
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
        lostItem.put("imageUrl", objectStorageUrlResolver.resolveReadUrl(item.getImageUrl()));
        dashboardOperationsStore.saveLostItem(lostItem);
    }

    private Map<String, Object> createSeatShell(int seatNum) {
        Map<String, Object> shell = new LinkedHashMap<>();
        shell.put("seatId", seatCode(seatNum));
        shell.put("seatNumber", seatNum);
        shell.put("status", "AVAILABLE");
        shell.put("statusLabel", statusLabel("AVAILABLE"));
        shell.put("lastUpdated", formatDashboardTime(nowInKorea()));
        shell.put("notes", "IoT 자동 생성 좌석");
        shell.put("abnormal", false);
        shell.put("issueType", "정상 이용");
        shell.put("detectedAt", formatDashboardTime(nowInKorea()));
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
        log.put("timestamp", formatDashboardTime(nowInKorea()));
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
            return "VACANT_LONG";
        }
        return "VACANT_LONG";
    }

    private boolean isAbnormalStatus(String status) {
        return "VACANT_LONG".equals(status) || "OBJECT_ONLY".equals(status) || "SENSOR_DELAY".equals(status);
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "OCCUPIED" -> "사용 중";
            case "AVAILABLE", "EMPTY" -> "비어있음";
            case "RESERVED" -> "발권됨";
            case "VACANT_LONG" -> "장시간 비움";
            case "OBJECT_ONLY" -> "물품 감지";
            case "SENSOR_DELAY" -> "센서 지연";
            default -> status;
        };
    }

    private String baseStatusFor(boolean checkedIn, boolean occupied) {
        if (checkedIn && !occupied) {
            return "RESERVED";
        }
        return occupied ? "OCCUPIED" : "AVAILABLE";
    }

    private int normalizeThreshold(int thresholdMinutes) {
        if (thresholdMinutes < 10 || thresholdMinutes > 240) {
            return DEFAULT_SQUATTING_THRESHOLD_MINUTES;
        }
        return thresholdMinutes;
    }

    private Map<String, Object> squattingThresholdPayload(int thresholdMinutes) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("triggeredAt", formatAppTime(nowInKorea()));
        if (thresholdMinutes == 10) {
            payload.put("limit_seconds", 10);
        } else {
            payload.put("limit_minutes", thresholdMinutes);
        }
        return payload;
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
        if (posture == null || posture.isBlank()) {
            return "정상";
        }
        if (posture.contains("거북목") || posture.contains("허리") || posture.contains("숙임")) {
            return "허리 숙임";
        }
        return posture;
    }

    private String formatDashboardTime(LocalDateTime time) {
        return DASHBOARD_TIME_FORMATTER.format(time == null ? nowInKorea() : time);
    }

    private LocalDateTime toLocalDateTime(OffsetDateTime offsetDateTime) {
        return offsetDateTime.atZoneSameInstant(KOREA_ZONE).toLocalDateTime();
    }

    private String formatAppTime(LocalDateTime time) {
        return APP_TIME_FORMATTER.format(time == null ? nowInKorea() : time);
    }

    private LocalDateTime nowInKorea() {
        return LocalDateTime.now(KOREA_ZONE);
    }

    private void normalizeSeatMetadata(Seat seat) {
        String expectedLocation = seatLocation(seat.getSeatNum());
        String expectedCode = seatCode(seat.getSeatNum());
        boolean changed = false;
        if (!expectedLocation.equals(seat.getLocation())) {
            seat.setLocation(expectedLocation);
            changed = true;
        }
        if (!expectedCode.equals(seat.getSeatCode())) {
            seat.setSeatCode(expectedCode);
            changed = true;
        }
        if (changed) {
            seatRepository.save(seat);
        }
    }

    private void updateSeatOccupancyState(Seat seat, boolean occupied, LocalDateTime sensorTimestamp) {
        seat.setOccupied(occupied);
        if (!seat.isCheckedIn()) {
            seat.setVacantSince(null);
            seat.setStatus("AVAILABLE");
            return;
        }

        if (occupied) {
            seat.setVacantSince(null);
            seat.setStatus("OCCUPIED");
            return;
        }

        if (seat.getVacantSince() == null) {
            seat.setVacantSince(sensorTimestamp);
        }

        seat.setStatus("VACANT_LONG");
    }

    private void appendSquattingWarningIfNeeded(Seat seat, String previousStatus, LocalDateTime eventTime) {
        if (!"VACANT_LONG".equals(seat.getStatus()) || seat.getVacantSince() == null) {
            return;
        }

        long vacantMinutes = ChronoUnit.MINUTES.between(seat.getVacantSince(), eventTime);
        if (vacantMinutes < getSquattingThresholdMinutes()) {
            return;
        }

        Warning lastWarning = warningRepository.findFirstBySeatNumAndStatusOrderByWarningTimeDesc(
                seat.getSeatNum(),
                "vacant_long"
        );
        if (lastWarning != null && !lastWarning.getWarningTime().isBefore(seat.getVacantSince())) {
            return;
        }

        Warning warning = warningRepository.save(new Warning(
                seat,
                seat.getSeatNum(),
                "VACANT_LONG",
                "vacant_long",
                "Seat " + seat.getSeatNum() + " exceeded vacant threshold",
                eventTime
        ));
        syncWarningToDashboard(warning);
        prependSensorLog(
                "VACANT_LONG",
                seatCode(seat.getSeatNum()),
                "edge-ec2",
                String.valueOf(calculateVacantMinutes(seat)),
                "발권 후 압력 미감지 시간이 기준을 초과했습니다.",
                "지연"
        );
    }

    private int calculateVacantMinutes(Seat seat) {
        if (!seat.isCheckedIn() || seat.isOccupied() || seat.getVacantSince() == null) {
            return 0;
        }
        return (int) Math.max(0, ChronoUnit.MINUTES.between(seat.getVacantSince(), seat.getUpdatedAt()));
    }

    private String dashboardIssueType(String status) {
        return switch (status) {
            case "VACANT_LONG" -> "장시간 자리 비움";
            case "OBJECT_ONLY" -> "물품만 감지됨";
            case "SENSOR_DELAY" -> "센서 수집 지연";
            default -> "정상 이용";
        };
    }

    private String dashboardNotes(String status) {
        return switch (status) {
            case "VACANT_LONG" -> "발권 또는 착석 이력 이후 압력 미감지 시간이 기준을 초과했습니다.";
            case "OBJECT_ONLY" -> "사람 없이 물품만 감지된 좌석입니다.";
            case "SENSOR_DELAY" -> "센서 데이터 수집이 지연되고 있습니다.";
            default -> "정상 이용 중";
        };
    }

    private String dashboardWarningType(Warning warning) {
        return switch (warning.getStatus().toLowerCase(Locale.ROOT)) {
            case "lost_item" -> "분실물 감지";
            case "admin_warning" -> "관리자 경고";
            case "vacant_long" -> "장시간 비움";
            default -> "좌석 경고";
        };
    }
}
