package com.example.librarydashboard.adapter.out.memory;

import com.example.librarydashboard.entity.Seat;
import com.example.librarydashboard.entity.SeatUsage;
import com.example.librarydashboard.entity.User;
import com.example.librarydashboard.port.out.StudentAccountStore;
import com.example.librarydashboard.repository.SeatRepository;
import com.example.librarydashboard.repository.SeatUsageRepository;
import com.example.librarydashboard.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryStudentAccountStore implements StudentAccountStore {

    private final UserRepository userRepository;
    private final SeatRepository seatRepository;
    private final SeatUsageRepository seatUsageRepository;
    private final PasswordEncoder passwordEncoder;
    private final Map<String, Long> userIdByToken = new ConcurrentHashMap<>();

    public InMemoryStudentAccountStore(
            UserRepository userRepository,
            SeatRepository seatRepository,
            SeatUsageRepository seatUsageRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.seatRepository = seatRepository;
        this.seatUsageRepository = seatUsageRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    void seedDefaults() {
        ensureStudent("김도서", "20240001", "student1@library.com");
        ensureStudent("이열람", "20240002", "student2@library.com");
        ensureStudent("박자료", "20240003", "student3@library.com");
        ensureStudent("최좌석", "20240004", "student4@library.com");
    }

    @Override
    public Optional<Map<String, Object>> findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email).map(this::toMap);
    }

    @Override
    public Optional<Map<String, Object>> findById(String userId) {
        try {
            return userRepository.findById(Long.parseLong(userId)).map(this::toMap);
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    @Override
    public Map<String, Object> save(Map<String, Object> student) {
        User user = resolveUser(student.get("id"));
        user.setName(stringValue(student.get("name")));
        user.setStudentId(stringValue(student.get("studentId")));
        user.setEmail(normalizeEmail(stringValue(student.get("email"))));
        if (student.containsKey("password")) {
            user.setPassword(stringValue(student.get("password")));
        }
        user.setRole(stringValueOrDefault(student.get("role"), "USER"));
        user.setPhoto(stringValue(student.get("photo")));
        user.setAgreedToPrivacy(booleanValue(student.get("agreedToPrivacy")));
        return toMap(userRepository.save(user));
    }

    @Override
    public Optional<Map<String, Object>> findByToken(String token) {
        Long userId = userIdByToken.get(token);
        if (userId == null) {
            return Optional.empty();
        }
        return userRepository.findById(userId).map(this::toMap);
    }

    @Override
    public Optional<Map<String, Object>> findBySelectedSeatId(String seatId) {
        Integer seatNum = parseSeatNumber(seatId);
        if (seatNum == null) {
            return Optional.empty();
        }
        Optional<Seat> seat = seatRepository.findBySeatNum(seatNum);
        if (seat.isEmpty()) {
            return Optional.empty();
        }
        return seatUsageRepository.findFirstBySeatIdAndCheckOutTimeIsNullOrderByCheckInTimeDesc(seat.get().getId())
                .map(SeatUsage::getUser)
                .map(this::toMap);
    }

    @Override
    public void saveSessionToken(String token, String userId) {
        userIdByToken.put(token, Long.parseLong(userId));
    }

    @Override
    public void resetForTesting() {
        userIdByToken.clear();
    }

    private User resolveUser(Object idValue) {
        if (idValue == null) {
            return new User();
        }
        try {
            long userId = Long.parseLong(String.valueOf(idValue));
            return userRepository.findById(userId).orElseGet(User::new);
        } catch (NumberFormatException exception) {
            return new User();
        }
    }

    private void ensureStudent(String name, String studentId, String email) {
        if (userRepository.existsByStudentId(studentId)) {
            return;
        }
        userRepository.save(new User(
                email,
                name,
                studentId,
                passwordEncoder.encode("password123"),
                "USER",
                true,
                null
        ));
    }

    private Map<String, Object> toMap(User user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", user.getId());
        map.put("name", user.getName());
        map.put("studentId", user.getStudentId());
        map.put("email", user.getEmail());
        map.put("password", user.getPassword());
        map.put("role", user.getRole());
        map.put("agreedToPrivacy", Boolean.TRUE.equals(user.getAgreedToPrivacy()));
        map.put("photo", user.getPhoto());
        map.put("createdAt", user.getCreatedAt());
        return map;
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String stringValueOrDefault(Object value, String fallback) {
        String converted = stringValue(value);
        return converted == null || converted.isBlank() ? fallback : converted;
    }

    private boolean booleanValue(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    private Integer parseSeatNumber(String seatId) {
        try {
            return Integer.parseInt(seatId.replace("seat-", "").trim());
        } catch (RuntimeException exception) {
            return null;
        }
    }
}
