package com.example.librarydashboard.service;

import com.example.librarydashboard.dto.seat.AlertResponse;
import com.example.librarydashboard.dto.seat.CheckInStatusResponse;
import com.example.librarydashboard.dto.seat.LostItemResponse;
import com.example.librarydashboard.dto.seat.LostItemSaveRequest;
import com.example.librarydashboard.dto.seat.MessageResponse;
import com.example.librarydashboard.dto.seat.SeatSquattingRequest;
import com.example.librarydashboard.dto.seat.SeatStatusUpdateRequest;
import com.example.librarydashboard.dto.seat.SeatSummaryResponse;
import com.example.librarydashboard.entity.LostItem;
import com.example.librarydashboard.entity.LostItemLog;
import com.example.librarydashboard.entity.Seat;
import com.example.librarydashboard.entity.Warning;
import com.example.librarydashboard.port.out.DashboardOperationsStore;
import com.example.librarydashboard.repository.LostItemLogRepository;
import com.example.librarydashboard.repository.LostItemRepository;
import com.example.librarydashboard.repository.SeatRepository;
import com.example.librarydashboard.repository.WarningRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Transactional
public class SeatApiService {

    private static final DateTimeFormatter DASHBOARD_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm", Locale.KOREAN);

    private final SeatRepository seatRepository;
    private final WarningRepository warningRepository;
    private final LostItemRepository lostItemRepository;
    private final LostItemLogRepository lostItemLogRepository;
    private final DashboardOperationsStore dashboardOperationsStore;
    private final CheckInStatusService checkInStatusService;

    public SeatApiService(
            SeatRepository seatRepository,
            WarningRepository warningRepository,
            LostItemRepository lostItemRepository,
            LostItemLogRepository lostItemLogRepository,
            DashboardOperationsStore dashboardOperationsStore,
            CheckInStatusService checkInStatusService
    ) {
        this.seatRepository = seatRepository;
        this.warningRepository = warningRepository;
        this.lostItemRepository = lostItemRepository;
        this.lostItemLogRepository = lostItemLogRepository;
        this.dashboardOperationsStore = dashboardOperationsStore;
        this.checkInStatusService = checkInStatusService;
    }

    public MessageResponse updateSeatStatus(SeatStatusUpdateRequest request) {
        Seat seat = seatRepository.findBySeatNum(request.seatNum())
                .orElseGet(() -> new Seat(
                        request.seatNum(),
                        seatLocation(request.seatNum()),
                        seatCode(request.seatNum()),
                        0,
                        "AVAILABLE",
                        request.timestamp()
                ));

        String status = request.pressure() == 0 ? "AVAILABLE" : "OCCUPIED";
        seat.setPressure(request.pressure());
        seat.setStatus(status);
        seat.setUpdatedAt(request.timestamp());
        seatRepository.save(seat);

        syncSeatToDashboard(seat);
        return new MessageResponse("seat status updated");
    }

    public MessageResponse markSquatting(SeatSquattingRequest request) {
        Seat seat = seatRepository.findBySeatNum(request.seatNum())
                .orElseGet(() -> new Seat(
                        request.seatNum(),
                        seatLocation(request.seatNum()),
                        seatCode(request.seatNum()),
                        0,
                        "AVAILABLE",
                        LocalDateTime.now()
                ));

        String status = normalizeSquattingStatus(request.status());
        seat.setStatus(status);
        seat.setUpdatedAt(LocalDateTime.now());
        seatRepository.save(seat);

        Warning alert = warningRepository.save(new Warning(
                seat,
                "SQUATTING",
                "OPEN",
                "Seat " + request.seatNum() + " marked as " + status
        ));

        syncSeatToDashboard(seat);
        syncAlertToDashboard(alert);
        return new MessageResponse("squatting detected");
    }

    public MessageResponse saveLostItem(LostItemSaveRequest request) {
        Seat seat = seatRepository.findBySeatNum(request.seatNum())
                .orElseGet(() -> seatRepository.save(new Seat(
                        request.seatNum(),
                        seatLocation(request.seatNum()),
                        seatCode(request.seatNum()),
                        0,
                        "AVAILABLE",
                        LocalDateTime.now()
                )));

        LostItem saved = lostItemRepository.save(new LostItem(
                seat,
                request.imageUrl()
        ));
        request.detectedObjects().forEach(object -> lostItemLogRepository.save(new LostItemLog(saved, object)));

        syncLostItemToDashboard(saved, request.detectedObjects());
        return new MessageResponse("lost item saved");
    }

    @Transactional(readOnly = true)
    public CheckInStatusResponse getCheckInStatus(int seatNum) {
        return new CheckInStatusResponse(seatNum, checkInStatusService.isCheckedIn(seatNum));
    }

    @Transactional(readOnly = true)
    public List<SeatSummaryResponse> getSeats() {
        return seatRepository.findAll().stream()
                .map(seat -> new SeatSummaryResponse(
                        seat.getSeatNum(),
                        seat.getStatus(),
                        seat.getPressure(),
                        seat.getUpdatedAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LostItemResponse> getLostItems() {
        return lostItemRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(item -> new LostItemResponse(
                        item.getId(),
                        item.getSeat() == null ? null : item.getSeat().getSeatNum(),
                        item.getImageUrl(),
                        lostItemLogRepository.findAllByLostItem_Id(item.getId()).stream().map(LostItemLog::getDetectedObject).toList(),
                        item.getCreatedAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> getAlerts() {
        return warningRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(alert -> new AlertResponse(
                        alert.getId(),
                        alert.getSeat() == null ? null : alert.getSeat().getSeatNum(),
                        alert.getWarningType(),
                        alert.getStatus(),
                        alert.getMessage(),
                        alert.getCreatedAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDatabaseTest() {
        return Map.of(
                "message", "database connected",
                "seatCount", seatRepository.count()
        );
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
        target.put("issueType", abnormal ? "사석화 감지" : "정상 이용");
        target.put("notes", abnormal ? "관리자 확인 필요" : "정상 이용 중");
        target.put("actionStatus", abnormal ? "대기" : "정상");
        target.put("pressureValue", seat.getPressure() == null ? 0.0d : seat.getPressure().doubleValue());
        target.put("personDetected", seat.getPressure() != null && seat.getPressure() > 0);
        target.put("objectDetected", abnormal);
        target.put("cameraConfidence", abnormal ? 0.90d : 0.95d);
        target.put("gateway", "edge-ec2");
        target.put("durationMinutes", abnormal ? 1 : 0);
        target.put("sensorHint", "pressure " + (seat.getPressure() == null ? 0 : seat.getPressure()));
        dashboardOperationsStore.saveSeat(target);
    }

    private void syncAlertToDashboard(Warning alert) {
        Map<String, Object> history = new LinkedHashMap<>();
        history.put("id", "ALERT-" + alert.getId());
        history.put("seatId", alert.getSeat() == null ? null : seatCode(alert.getSeat().getSeatNum()));
        history.put("studentIdMasked", "TEMP");
        history.put("messageType", "사석화 감지");
        history.put("channel", "IoT");
        history.put("createdAt", formatDashboardTime(alert.getCreatedAt()));
        history.put("status", "전송 완료");
        history.put("message", alert.getMessage());
        dashboardOperationsStore.prependAlertHistory(history);
    }

    private void syncLostItemToDashboard(LostItem item, List<String> detectedObjects) {
        Map<String, Object> lostItem = new LinkedHashMap<>();
        lostItem.put("itemId", "LOST-" + item.getId());
        lostItem.put("category", firstDetectedObject(detectedObjects));
        lostItem.put("foundAt", formatDashboardTime(item.getCreatedAt()));
        lostItem.put("zone", "IoT 구역");
        lostItem.put("seatId", item.getSeat() == null ? null : seatCode(item.getSeat().getSeatNum()));
        lostItem.put("description", String.join(", ", detectedObjects));
        lostItem.put("status", "보관 중");
        lostItem.put("custodian", "IoT 자동 등록");
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
        shell.put("sensorHint", "pressure 0");
        shell.put("actionStatus", "정상");
        shell.put("pressureValue", 0.0d);
        shell.put("personDetected", false);
        shell.put("objectDetected", false);
        shell.put("cameraConfidence", 0.0d);
        shell.put("gateway", "edge-ec2");
        return shell;
    }

    private String normalizeSquattingStatus(String status) {
        if ("squatting".equalsIgnoreCase(status)) {
            return "SQUATTING";
        }
        if ("abnormal".equalsIgnoreCase(status)) {
            return "ABNORMAL";
        }
        return "SQUATTING";
    }

    private boolean isAbnormalStatus(String status) {
        return "SQUATTING".equals(status) || "ABNORMAL".equals(status) || "RESERVED".equals(status);
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "OCCUPIED" -> "사용 중";
            case "AVAILABLE", "EMPTY" -> "비어있음";
            case "SQUATTING", "ABNORMAL", "RESERVED" -> "사석화 감지";
            default -> status;
        };
    }

    private String seatCode(int seatNum) {
        return "seat-" + seatNum;
    }

    private String seatLocation(int seatNum) {
        return "IoT Zone Seat " + seatNum;
    }

    private String formatDashboardTime(LocalDateTime time) {
        return DASHBOARD_TIME_FORMATTER.format(time);
    }

    private String firstDetectedObject(List<String> detectedObjects) {
        if (detectedObjects.isEmpty()) {
            return "미분류";
        }
        return detectedObjects.get(0);
    }
}
