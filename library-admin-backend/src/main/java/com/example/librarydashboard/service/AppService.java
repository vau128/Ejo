package com.example.librarydashboard.service;

import com.example.librarydashboard.dto.AppSettingsRequest;
import com.example.librarydashboard.dto.StudentLoginRequest;
import com.example.librarydashboard.dto.StudentSignupRequest;
import com.example.librarydashboard.dto.seat.AlertResponse;
import com.example.librarydashboard.entity.LostItem;
import com.example.librarydashboard.entity.PostureLog;
import com.example.librarydashboard.entity.Seat;
import com.example.librarydashboard.entity.SeatUsage;
import com.example.librarydashboard.entity.User;
import com.example.librarydashboard.port.out.DeviceEventGateway;
import com.example.librarydashboard.port.out.ObjectStorageUrlResolver;
import com.example.librarydashboard.port.out.SeatStore;
import com.example.librarydashboard.port.out.StudentAccountStore;
import com.example.librarydashboard.port.out.UserSettingsStore;
import com.example.librarydashboard.repository.LostItemRepository;
import com.example.librarydashboard.repository.PostureLogRepository;
import com.example.librarydashboard.repository.SeatRepository;
import com.example.librarydashboard.repository.SeatUsageRepository;
import com.example.librarydashboard.repository.UserRepository;
import com.example.librarydashboard.repository.WarningRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
@Transactional
public class AppService {

    private static final DateTimeFormatter DAY_LABEL_FORMATTER = DateTimeFormatter.ofPattern("M/d", Locale.KOREAN);
    private static final DateTimeFormatter APP_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.KOREAN);
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final Logger log = LoggerFactory.getLogger(AppService.class);

    private final StudentAccountStore studentAccountStore;
    private final SeatStore seatStore;
    private final UserSettingsStore userSettingsStore;
    private final DeviceEventGateway deviceEventGateway;
    private final ObjectStorageUrlResolver objectStorageUrlResolver;
    private final LostItemRepository lostItemRepository;
    private final SeatRepository seatRepository;
    private final SeatApiService seatApiService;
    private final WarningRepository warningRepository;
    private final UserRepository userRepository;
    private final SeatUsageRepository seatUsageRepository;
    private final PostureLogRepository postureLogRepository;

    public AppService(
            StudentAccountStore studentAccountStore,
            SeatStore seatStore,
            UserSettingsStore userSettingsStore,
            DeviceEventGateway deviceEventGateway,
            ObjectStorageUrlResolver objectStorageUrlResolver,
            LostItemRepository lostItemRepository,
            SeatRepository seatRepository,
            SeatApiService seatApiService,
            WarningRepository warningRepository,
            UserRepository userRepository,
            SeatUsageRepository seatUsageRepository,
            PostureLogRepository postureLogRepository
    ) {
        this.studentAccountStore = studentAccountStore;
        this.seatStore = seatStore;
        this.userSettingsStore = userSettingsStore;
        this.deviceEventGateway = deviceEventGateway;
        this.objectStorageUrlResolver = objectStorageUrlResolver;
        this.lostItemRepository = lostItemRepository;
        this.seatRepository = seatRepository;
        this.seatApiService = seatApiService;
        this.warningRepository = warningRepository;
        this.userRepository = userRepository;
        this.seatUsageRepository = seatUsageRepository;
        this.postureLogRepository = postureLogRepository;
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

    @Transactional(readOnly = true)
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
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 등록된 이메일입니다.");
        }
        if (userRepository.existsByStudentId(request.studentId().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 등록된 학번입니다.");
        }

        Map<String, Object> user = mapOf(
                "name", request.name().trim(),
                "studentId", request.studentId().trim(),
                "email", normalizedEmail,
                "password", request.password(),
                "role", "USER",
                "agreedToPrivacy", true,
                "photo", null
        );

        Map<String, Object> savedUser = studentAccountStore.save(user);
        userSettingsStore.save(String.valueOf(savedUser.get("id")), defaultSettings());
        return buildAuthResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCurrentUser(String token) {
        return mapOf("user", sanitizeUser(requireUser(token)));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSeats(String token) {
        Map<String, Object> user = requireUser(token);
        String selectedSeatId = activeSeatIdForUser(toLong(user.get("id")));
        seatApiService.bootstrapSeatsIfEmpty();
        List<Map<String, Object>> seatItems = seatRepository.findAll().stream()
                .filter(seat -> seat.getSeatNum() != null && seat.getSeatNum() >= 1 && seat.getSeatNum() <= 4)
                .sorted(Comparator.comparing(Seat::getSeatNum))
                .map(seat -> seatResponse(seat, selectedSeatId))
                .toList();
        return mapOf(
                "student", sanitizeUser(user),
                "seats", seatItems
        );
    }

    public Map<String, Object> toggleSeatSelection(String token, String seatId) {
        Map<String, Object> user = requireUser(token);
        User userEntity = requireUserEntity(user);
        String normalizedSeatId = normalizeSeatId(seatId);
        int seatNum = parseSeatNumber(normalizedSeatId);
        Seat seat = seatRepository.findBySeatNum(seatNum)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "좌석을 찾을 수 없습니다."));

        SeatUsage activeUsage = seatUsageRepository.findFirstByUserIdAndCheckOutTimeIsNullOrderByCheckInTimeDesc(userEntity.getId())
                .orElse(null);
        if (activeUsage != null && Objects.equals(activeUsage.getSeat().getId(), seat.getId())) {
            return releaseSeat(userEntity, activeUsage, seat, normalizedSeatId);
        }
        if (activeUsage != null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 " + activeUsage.getSeat().getSeatNum() + "번 좌석을 발권했습니다. 먼저 반납해주세요."
            );
        }

        if (seatUsageRepository.findFirstBySeatIdAndCheckOutTimeIsNullOrderByCheckInTimeDesc(seat.getId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, seat.getSeatNum() + "번 좌석은 이미 다른 학생이 사용 중입니다.");
        }
        if (!isSelectableStatus(seat.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    seat.getSeatNum() + "번 좌석은 " + seatStatusLabel(seat.getStatus()) + " 상태입니다."
            );
        }

        LocalDateTime now = nowInKorea();
        seatUsageRepository.save(new SeatUsage(seat, userEntity, now, null));

        seat.setCheckedIn(true);
        seat.setStatus(seat.isOccupied() ? "OCCUPIED" : "VACANT_LONG");
        seat.setVacantSince(seat.isOccupied() ? null : now);
        seat.setUpdatedAt(now);
        seatRepository.save(seat);
        seatApiService.syncSeatToDashboardState(seat);
        seatStore.assignSeatToUser(normalizedSeatId, String.valueOf(userEntity.getId()));
        publishSeatStatusChangedSafely(seatIdFromNumber(seat.getSeatNum()), "SELECTED", mapOf("userId", userEntity.getId()));

        return mapOf(
                "message", seat.getSeatNum() + "번 좌석이 발권되었습니다.",
                "selectedSeat", seatResponse(seat, normalizedSeatId),
                "currentUser", sanitizeUser(studentAccountStore.findById(String.valueOf(userEntity.getId())).orElse(user))
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getMySeat(String token) {
        Map<String, Object> user = requireUser(token);
        String selectedSeatId = activeSeatIdForUser(toLong(user.get("id")));
        if (selectedSeatId == null) {
            return mapOf("seat", null);
        }

        Seat seat = seatRepository.findBySeatNum(parseSeatNumber(selectedSeatId)).orElse(null);
        return mapOf("seat", seat == null ? null : seatResponse(seat, selectedSeatId));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getMyPostureStats(String token) {
        Map<String, Object> user = requireUser(token);
        long userId = toLong(user.get("id"));
        LocalDate today = LocalDate.now(KOREA_ZONE);
        LocalDate startDate = today.minusDays(6);
        LocalDateTime windowStart = startDate.atStartOfDay();
        LocalDateTime windowEnd = nowInKorea();

        List<SeatUsage> usages = seatUsageRepository.findAllByUserIdOrderByCheckInTimeAsc(userId).stream()
                .filter(usage -> overlaps(usage, windowStart, windowEnd))
                .toList();

        List<PostureLog> matchedLogs = new ArrayList<>();
        for (SeatUsage usage : usages) {
            LocalDateTime usageStart = max(windowStart, usage.getCheckInTime());
            LocalDateTime usageEnd = min(windowEnd, usage.getCheckOutTime() == null ? windowEnd : usage.getCheckOutTime());
            matchedLogs.addAll(postureLogRepository.findAllBySeatNumAndCreatedAtBetweenOrderByCreatedAtAsc(
                    usage.getSeat().getSeatNum(),
                    usageStart,
                    usageEnd
            ));
        }

        Map<String, Long> postureCounts = new LinkedHashMap<>();
        Map<LocalDate, List<PostureLog>> logsByDay = new LinkedHashMap<>();
        for (int offset = 0; offset < 7; offset++) {
            logsByDay.put(startDate.plusDays(offset), new ArrayList<>());
        }

        long normalCount = 0;
        for (PostureLog log : matchedLogs) {
            String posture = postureLabel(log.getPosture());
            postureCounts.put(posture, postureCounts.getOrDefault(posture, 0L) + 1L);
            LocalDate logDate = log.getCreatedAt().toLocalDate();
            logsByDay.computeIfAbsent(logDate, ignored -> new ArrayList<>()).add(log);
            if (isNormalPosture(log.getPosture())) {
                normalCount++;
            }
        }

        long totalCount = matchedLogs.size();
        long abnormalCount = totalCount - normalCount;
        List<Map<String, Object>> breakdown = postureCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(entry -> mapOf(
                        "label", entry.getKey(),
                        "count", entry.getValue(),
                        "percentage", totalCount == 0 ? 0 : (int) Math.round(entry.getValue() * 100.0d / totalCount)
                ))
                .toList();

        List<Map<String, Object>> daily = logsByDay.entrySet().stream()
                .map(entry -> {
                    List<PostureLog> dayLogs = entry.getValue();
                    long dayNormalCount = dayLogs.stream().filter(log -> isNormalPosture(log.getPosture())).count();
                    String dominantPosture = dayLogs.isEmpty()
                            ? "-"
                            : dayLogs.stream()
                            .collect(java.util.stream.Collectors.groupingBy(
                                    log -> postureLabel(log.getPosture()),
                                    LinkedHashMap::new,
                                    java.util.stream.Collectors.counting()
                            ))
                            .entrySet().stream()
                            .max(Map.Entry.comparingByValue())
                            .map(Map.Entry::getKey)
                            .orElse("-");

                    return mapOf(
                            "date", entry.getKey().toString(),
                            "label", DAY_LABEL_FORMATTER.format(entry.getKey()),
                            "totalCount", dayLogs.size(),
                            "normalCount", dayNormalCount,
                            "abnormalCount", dayLogs.size() - dayNormalCount,
                            "dominantPosture", dominantPosture
                    );
                })
                .toList();

        String currentSeatId = activeSeatIdForUser(userId);
        Integer currentSeatNumber = currentSeatId == null ? null : parseSeatNumber(currentSeatId);

        return mapOf(
                "rangeStart", startDate.toString(),
                "rangeEnd", today.toString(),
                "currentSeatNumber", currentSeatNumber,
                "totalSamples", totalCount,
                "normalSamples", normalCount,
                "abnormalSamples", abnormalCount,
                "mostFrequentPosture", postureCounts.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse("데이터 없음"),
                "breakdown", breakdown,
                "daily", daily
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getWarnings(String token) {
        Map<String, Object> user = requireUser(token);
        String selectedSeatId = activeSeatIdForUser(toLong(user.get("id")));
        if (selectedSeatId == null) {
            return mapOf(
                    "warningCount", 0,
                    "warnings", List.of()
            );
        }

        int seatNum = parseSeatNumber(selectedSeatId);
        List<AlertResponse> warnings = warningRepository.findAllBySeatNumOrderByWarningTimeDesc(seatNum).stream()
                .filter(warning -> !isLostItemWarning(warning.getWarningType(), warning.getStatus()))
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

        return mapOf(
                "warningCount", warnings.size(),
                "warnings", warnings
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getLostItems(String token) {
        Map<String, Object> user = requireUser(token);
        String selectedSeatId = activeSeatIdForUser(toLong(user.get("id")));
        if (selectedSeatId == null) {
            return mapOf("reports", List.of());
        }

        int seatNum = parseSeatNumber(selectedSeatId);
        return mapOf(
                "reports", lostItemRepository.findAllByOrderByDetectedTimeDesc().stream()
                        .filter(item -> Objects.equals(item.getSeatNum(), seatNum))
                        .map(this::lostItemReport)
                        .toList()
        );
    }

    @Transactional(readOnly = true)
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

    private User requireUserEntity(Map<String, Object> user) {
        return userRepository.findById(toLong(user.get("id")))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "학생 계정을 찾을 수 없습니다."));
    }

    private Map<String, Object> sanitizeUser(Map<String, Object> user) {
        long userId = toLong(user.get("id"));
        return mapOf(
                "id", String.valueOf(user.get("id")),
                "name", user.get("name"),
                "studentId", user.get("studentId"),
                "email", user.get("email"),
                "role", stringValueOrDefault(user.get("role"), "USER"),
                "photo", user.get("photo"),
                "createdAt", user.get("createdAt") == null ? null : String.valueOf(user.get("createdAt")),
                "warningCount", warningCountForUser(userId),
                "agreedToPrivacy", user.get("agreedToPrivacy")
        );
    }

    private Map<String, Object> lostItemReport(LostItem item) {
        return mapOf(
                "reportId", String.valueOf(item.getId()),
                "seatNumber", item.getSeatNum(),
                "detectedAt", formatAppTimestamp(item.getDetectedTime()),
                "imageAssetPath", objectStorageUrlResolver.resolveReadUrl(item.getImageUrl()),
                "classificationStatus", item.getCategory() == null || item.getCategory().isBlank() ? item.getStatus() : item.getCategory()
        );
    }

    private Map<String, Object> seatResponse(Seat seat, String selectedSeatId) {
        String seatId = seatIdFromNumber(seat.getSeatNum());
        LocalDateTime selectedAt = seatUsageRepository.findFirstBySeatIdAndCheckOutTimeIsNullOrderByCheckInTimeDesc(seat.getId())
                .map(SeatUsage::getCheckInTime)
                .orElse(null);
        return mapOf(
                "seatId", seatId,
                "seatNumber", seat.getSeatNum(),
                "location", seatLocation(seat.getSeatNum()),
                "status", seat.getStatus(),
                "statusLabel", seatStatusLabel(seat.getStatus()),
                "checkedIn", seat.isCheckedIn(),
                "occupied", seat.isOccupied(),
                "posture", postureLabel(seat.getPosture()),
                "leftPressure", seat.getLeftPressure() == null ? 0 : seat.getLeftPressure(),
                "rightPressure", seat.getRightPressure() == null ? 0 : seat.getRightPressure(),
                "backPressure", seat.getBackPressure() == null ? 0 : seat.getBackPressure(),
                "postureTimestamp", seat.getPostureTimestamp() == null ? null : formatAppTimestamp(seat.getPostureTimestamp()),
                "selectedAt", selectedAt == null ? null : formatAppTimestamp(selectedAt),
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

    public String maskStudentId(String studentId) {
        if (studentId == null || studentId.length() < 4) {
            return "익명";
        }
        return studentId.substring(0, 4) + "****";
    }

    private Map<String, Object> releaseSeat(User user, SeatUsage activeUsage, Seat seat, String seatId) {
        LocalDateTime now = nowInKorea();
        activeUsage.setCheckOutTime(now);
        seatUsageRepository.save(activeUsage);

        seat.setCheckedIn(false);
        seat.setOccupied(false);
        seat.setStatus("AVAILABLE");
        seat.setVacantSince(null);
        seat.setUpdatedAt(now);
        seatRepository.save(seat);
        seatApiService.syncSeatToDashboardState(seat);
        seatStore.releaseSeatFromUser(String.valueOf(user.getId()));
        publishSeatStatusChangedSafely(seatIdFromNumber(seat.getSeatNum()), "RELEASED", mapOf("userId", user.getId()));

        Map<String, Object> latestUser = studentAccountStore.findById(String.valueOf(user.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "학생 계정을 찾을 수 없습니다."));

        return mapOf(
                "message", seat.getSeatNum() + "번 좌석이 반납되었습니다.",
                "selectedSeat", null,
                "currentUser", sanitizeUser(latestUser)
        );
    }

    private boolean isSelectableStatus(String status) {
        return Objects.equals(status, "AVAILABLE")
                || Objects.equals(status, "RESERVED")
                || Objects.equals(status, "OCCUPIED");
    }

    private long warningCountForUser(long userId) {
        String selectedSeatId = activeSeatIdForUser(userId);
        if (selectedSeatId == null) {
            return 0;
        }
        return warningRepository.findAllBySeatNumOrderByWarningTimeDesc(parseSeatNumber(selectedSeatId)).stream()
                .filter(warning -> !isLostItemWarning(warning.getWarningType(), warning.getStatus()))
                .count();
    }

    private String activeSeatIdForUser(long userId) {
        return seatUsageRepository.findFirstByUserIdAndCheckOutTimeIsNullOrderByCheckInTimeDesc(userId)
                .map(usage -> seatIdFromNumber(usage.getSeat().getSeatNum()))
                .orElse(null);
    }

    private boolean overlaps(SeatUsage usage, LocalDateTime start, LocalDateTime end) {
        if (usage.getCheckInTime() == null) {
            return false;
        }
        LocalDateTime usageEnd = usage.getCheckOutTime() == null ? end : usage.getCheckOutTime();
        return !usage.getCheckInTime().isAfter(end) && !usageEnd.isBefore(start);
    }

    private LocalDateTime max(LocalDateTime left, LocalDateTime right) {
        return left.isAfter(right) ? left : right;
    }

    private LocalDateTime min(LocalDateTime left, LocalDateTime right) {
        return left.isBefore(right) ? left : right;
    }

    private boolean isNormalPosture(String posture) {
        if (posture == null || posture.isBlank()) {
            return true;
        }
        String normalized = posture.toLowerCase(Locale.ROOT);
        return normalized.contains("정상") || normalized.contains("normal");
    }

    private String postureLabel(String posture) {
        if (posture == null || posture.isBlank()) {
            return "정상";
        }
        if (isNormalPosture(posture)) {
            return "정상";
        }
        if (posture.contains("거북목") || posture.contains("허리") || posture.contains("숙임")) {
            return "허리 숙임";
        }
        if (posture.contains("왼")) {
            return "왼쪽 기울어짐";
        }
        if (posture.contains("오른")) {
            return "오른쪽 기울어짐";
        }
        return posture;
    }

    private Map<String, Object> defaultSettings() {
        return new LinkedHashMap<>(mapOf(
                "pushEnabled", true,
                "seatAlertEnabled", true,
                "warningAlertEnabled", true
        ));
    }

    private void publishSeatStatusChangedSafely(String seatId, String status, Map<String, Object> payload) {
        try {
            deviceEventGateway.publishSeatStatusChanged(seatId, status, payload);
        } catch (RuntimeException exception) {
            log.warn("Failed to publish seat status change for seatId={} status={}", seatId, status, exception);
        }
    }

    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            map.put(String.valueOf(values[index]), values[index + 1]);
        }
        return map;
    }

    private long toLong(Object value) {
        return Long.parseLong(String.valueOf(value));
    }

    private String stringValueOrDefault(Object value, String fallback) {
        return value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value);
    }

    private LocalDateTime nowInKorea() {
        return LocalDateTime.now(KOREA_ZONE);
    }

    private String formatAppTimestamp(LocalDateTime time) {
        return APP_TIMESTAMP_FORMATTER.format(time == null ? nowInKorea() : time);
    }

    private String seatLocation(int seatNum) {
        return "A-" + seatNum;
    }

    private boolean isLostItemWarning(String warningType, String status) {
        return "lost_item".equalsIgnoreCase(warningType) || "lost_item".equalsIgnoreCase(status);
    }

    private int parseSeatNumber(String seatId) {
        return Integer.parseInt(seatId.replace("seat-", "").trim());
    }

    private String normalizeSeatId(String seatId) {
        if (seatId == null || seatId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "좌석 ID가 필요합니다.");
        }
        return seatId.trim().toLowerCase(Locale.ROOT);
    }

    private String seatIdFromNumber(int seatNum) {
        return "seat-" + seatNum;
    }
}
