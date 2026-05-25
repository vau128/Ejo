package com.example.librarydashboard.service;

import com.example.librarydashboard.dto.AppSettingsRequest;
import com.example.librarydashboard.dto.StudentLoginRequest;
import com.example.librarydashboard.dto.StudentSignupRequest;
import com.example.librarydashboard.dto.seat.AlertResponse;
import com.example.librarydashboard.entity.Seat;
import com.example.librarydashboard.port.out.DeviceEventGateway;
import com.example.librarydashboard.port.out.LostItemStore;
import com.example.librarydashboard.port.out.SeatStore;
import com.example.librarydashboard.port.out.StudentAccountStore;
import com.example.librarydashboard.port.out.UserSettingsStore;
import com.example.librarydashboard.repository.SeatRepository;
import com.example.librarydashboard.repository.WarningRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.time.LocalDateTime;

@Service
public class AppService {

    private final StudentAccountStore studentAccountStore;
    private final SeatStore seatStore;
    private final LostItemStore lostItemStore;
    private final UserSettingsStore userSettingsStore;
    private final DeviceEventGateway deviceEventGateway;
    private final SeatRepository seatRepository;
    private final SeatApiService seatApiService;
    private final WarningRepository warningRepository;

    public AppService(
            StudentAccountStore studentAccountStore,
            SeatStore seatStore,
            LostItemStore lostItemStore,
            UserSettingsStore userSettingsStore,
            DeviceEventGateway deviceEventGateway,
            SeatRepository seatRepository,
            SeatApiService seatApiService,
            WarningRepository warningRepository
    ) {
        this.studentAccountStore = studentAccountStore;
        this.seatStore = seatStore;
        this.lostItemStore = lostItemStore;
        this.userSettingsStore = userSettingsStore;
        this.deviceEventGateway = deviceEventGateway;
        this.seatRepository = seatRepository;
        this.seatApiService = seatApiService;
        this.warningRepository = warningRepository;
    }

    public String resolveToken(String authorization, String studentToken) {
        if (studentToken != null && !studentToken.isBlank()) {
            return studentToken.trim();
        }
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7).trim();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "학생 인증 토큰이 필요합니다.");
    }

    public Map<String, Object> login(StudentLoginRequest request) {
        Map<String, Object> user = studentAccountStore
                .findByEmail(request.email().trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호를 확인해주세요."));
        if (!Objects.equals(user.get("password"), request.password())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호를 확인해주세요.");
        }
        return buildAuthResponse(user);
    }

    public Map<String, Object> signup(StudentSignupRequest request) {
        if (!request.agreedToPrivacy()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "정보 동의 후 회원가입이 가능합니다.");
        }

        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        if (studentAccountStore.findByEmail(normalizedEmail).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 등록된 이메일입니다.");
        }

        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", "student-local-" + System.currentTimeMillis());
        user.put("name", request.name().trim());
        user.put("studentId", request.studentId().trim());
        user.put("email", normalizedEmail);
        user.put("password", request.password());
        user.put("warningCount", 0);
        user.put("agreedToPrivacy", true);
        user.put("selectedSeatId", null);

        Map<String, Object> savedUser = studentAccountStore.save(user);
        userSettingsStore.save(String.valueOf(savedUser.get("id")), defaultSettings());
        return buildAuthResponse(savedUser);
    }

    public Map<String, Object> getCurrentUser(String token) {
        return mapOf("user", sanitizeUser(requireUser(token)));
    }

    public Map<String, Object> getSeats(String token) {
        Map<String, Object> user = requireUser(token);
        String selectedSeatId = stringValue(user.get("selectedSeatId"));
        seatApiService.bootstrapSeatsIfEmpty();
        List<Seat> seats = seatRepository.findAll().stream()
                .filter(seat -> seat.getSeatNum() != null && seat.getSeatNum() >= 1 && seat.getSeatNum() <= 4)
                .sorted((left, right) -> Integer.compare(left.getSeatNum(), right.getSeatNum()))
                .toList();
        List<Map<String, Object>> seatItems = seats.stream()
                .map(seat -> seatResponse(seat, selectedSeatId))
                .toList();

        long availableCount = seats.stream().filter(seat -> Objects.equals(seat.getStatus(), "AVAILABLE")).count();
        long reservedCount = seats.stream().filter(seat -> Objects.equals(seat.getStatus(), "RESERVED")).count();
        long occupiedCount = seats.stream().filter(seat -> Objects.equals(seat.getStatus(), "OCCUPIED")).count();
        long squattingCount = seats.stream().filter(seat ->
                Objects.equals(seat.getStatus(), "VACANT_LONG")
                        || Objects.equals(seat.getStatus(), "OBJECT_ONLY")
                        || Objects.equals(seat.getStatus(), "SENSOR_DELAY")
        ).count();

        return mapOf(
                "summary", mapOf(
                        "totalSeats", seats.size(),
                        "availableSeats", availableCount,
                        "reservedSeats", reservedCount,
                        "occupiedSeats", occupiedCount,
                        "squattingSeats", squattingCount
                ),
                "seats", seatItems
        );
    }

    public Map<String, Object> toggleSeatSelection(String token, String seatId) {
        Map<String, Object> user = requireUser(token);
        int seatNumber = parseSeatNumber(seatId);
        Seat seat = seatRepository.findBySeatNum(seatNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "좌석을 찾을 수 없습니다."));
        String normalizedSeatId = "seat-" + seatNumber;

        String selectedSeatId = stringValue(user.get("selectedSeatId"));
        if (Objects.equals(selectedSeatId, normalizedSeatId)) {
            user.put("selectedSeatId", null);
            studentAccountStore.save(user);
            seatStore.releaseSeatFromUser(String.valueOf(user.get("id")));
            seat.setCheckedIn(false);
            seat.setVacantSince(null);
            seat.setUpdatedAt(LocalDateTime.now());
            if (!seat.isOccupied()) {
                seat.setStatus("AVAILABLE");
            }
            seatRepository.save(seat);
            seatApiService.syncSeatToDashboardState(seat);
            deviceEventGateway.publishSeatStatusChanged(normalizedSeatId, "RELEASED", mapOf("userId", user.get("id")));
            return mapOf(
                    "message", "좌석 선택이 취소되었습니다.",
                    "selectedSeat", null
            );
        }

        if (selectedSeatId != null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 " + parseSeatNumber(selectedSeatId) + "번 좌석을 선택했습니다. 먼저 기존 좌석 선택을 취소해주세요."
            );
        }

        if (!Objects.equals(seat.getStatus(), "AVAILABLE")) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    seat.getSeatNum() + "번 좌석은 " + seatStatusLabel(seat.getStatus()) + " 상태입니다."
            );
        }

        user.put("selectedSeatId", normalizedSeatId);
        studentAccountStore.save(user);
        seat.setCheckedIn(true);
        seat.setStatus("RESERVED");
        seat.setVacantSince(seat.isOccupied() ? null : LocalDateTime.now());
        seat.setUpdatedAt(LocalDateTime.now());
        seatRepository.save(seat);
        seatApiService.syncSeatToDashboardState(seat);
        seatStore.assignSeatToUser(normalizedSeatId, String.valueOf(user.get("id")));
        deviceEventGateway.publishSeatStatusChanged(normalizedSeatId, "SELECTED", mapOf("userId", user.get("id")));
        return mapOf(
                "message", seat.getSeatNum() + "번 좌석이 선택되었습니다.",
                "selectedSeat", seatResponse(seat, normalizedSeatId)
        );
    }

    public Map<String, Object> getMySeat(String token) {
        Map<String, Object> user = requireUser(token);
        String selectedSeatId = stringValue(user.get("selectedSeatId"));
        if (selectedSeatId == null) {
            return mapOf("seat", null);
        }

        Seat seat = seatRepository.findBySeatNum(parseSeatNumber(selectedSeatId)).orElse(null);
        return mapOf("seat", seat == null ? null : seatResponse(seat, selectedSeatId));
    }

    public Map<String, Object> getWarnings(String token) {
        Map<String, Object> user = requireUser(token);
        String selectedSeatId = stringValue(user.get("selectedSeatId"));
        if (selectedSeatId == null) {
            user.put("warningCount", 0);
            studentAccountStore.save(user);
            return mapOf(
                    "warningCount", 0,
                    "warnings", List.of()
            );
        }

        int seatNum = parseSeatNumber(selectedSeatId);
        List<AlertResponse> warnings = warningRepository.findAllBySeatNumOrderByWarningTimeDesc(seatNum).stream()
                .map(warning -> new AlertResponse(
                        warning.getId(),
                        warning.getSeatNum(),
                        warning.getWarningType(),
                        warning.getStatus(),
                        warning.getMessage(),
                        warning.getWarningTime()
                ))
                .sorted(Comparator.comparing(AlertResponse::warningTime).reversed())
                .toList();

        user.put("warningCount", warnings.size());
        studentAccountStore.save(user);
        return mapOf(
                "warningCount", warnings.size(),
                "warnings", warnings
        );
    }

    public Map<String, Object> getLostItems(String token) {
        requireUser(token);
        return mapOf("reports", lostItemStore.findAll());
    }

    public Map<String, Object> getSettings(String token) {
        Map<String, Object> user = requireUser(token);
        return mapOf("settings", userSettingsStore.findByUserIdOrDefault(String.valueOf(user.get("id"))));
    }

    public Map<String, Object> updateSettings(String token, AppSettingsRequest request) {
        Map<String, Object> user = requireUser(token);
        Map<String, Object> settings = userSettingsStore.findByUserIdOrDefault(String.valueOf(user.get("id")));
        settings.put("pushEnabled", request.pushEnabled());
        settings.put("seatAlertEnabled", request.seatAlertEnabled());
        settings.put("warningAlertEnabled", request.warningAlertEnabled());
        Map<String, Object> savedSettings = userSettingsStore.save(String.valueOf(user.get("id")), settings);
        return mapOf(
                "message", "앱 설정이 저장되었습니다.",
                "settings", savedSettings
        );
    }

    private Map<String, Object> buildAuthResponse(Map<String, Object> user) {
        String token = UUID.randomUUID().toString();
        studentAccountStore.saveSessionToken(token, String.valueOf(user.get("id")));
        return mapOf(
                "token", token,
                "user", sanitizeUser(user)
        );
    }

    private Map<String, Object> requireUser(String token) {
        return studentAccountStore.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "학생 인증 세션이 만료되었거나 유효하지 않습니다."));
    }

    private Map<String, Object> sanitizeUser(Map<String, Object> user) {
        return mapOf(
                "id", user.get("id"),
                "name", user.get("name"),
                "studentId", user.get("studentId"),
                "email", user.get("email"),
                "warningCount", user.get("warningCount"),
                "agreedToPrivacy", user.get("agreedToPrivacy")
        );
    }

    private Map<String, Object> seatResponse(Seat seat, String selectedSeatId) {
        String seatId = seatIdFromNumber(seat.getSeatNum());
        return mapOf(
                "seatId", seatId,
                "seatNumber", seat.getSeatNum(),
                "status", seat.getStatus(),
                "statusLabel", seatStatusLabel(seat.getStatus()),
                "checkedIn", seat.isCheckedIn(),
                "posture", seat.getPosture() == null ? "정상" : seat.getPosture(),
                "leftPressure", seat.getLeftPressure() == null ? 0 : seat.getLeftPressure(),
                "rightPressure", seat.getRightPressure() == null ? 0 : seat.getRightPressure(),
                "backPressure", seat.getBackPressure() == null ? 0 : seat.getBackPressure(),
                "selectedByCurrentUser", Objects.equals(seatId, selectedSeatId)
        );
    }

    private String seatStatusLabel(String status) {
        return switch (status) {
            case "AVAILABLE" -> "빈 좌석";
            case "RESERVED" -> "발권됨";
            case "OCCUPIED" -> "사용 중";
            case "VACANT_LONG" -> "장시간 비움";
            case "OBJECT_ONLY" -> "물품 감지";
            case "SENSOR_DELAY" -> "센서 지연";
            default -> "알 수 없음";
        };
    }

    private Map<String, Object> defaultSettings() {
        return new LinkedHashMap<>(mapOf(
                "pushEnabled", true,
                "seatAlertEnabled", true,
                "warningAlertEnabled", false
        ));
    }

    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            map.put(String.valueOf(values[index]), values[index + 1]);
        }
        return map;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private int parseSeatNumber(String seatId) {
        return Integer.parseInt(seatId.replace("seat-", "").trim());
    }

    private String seatIdFromNumber(int seatNum) {
        return "seat-" + seatNum;
    }
}
