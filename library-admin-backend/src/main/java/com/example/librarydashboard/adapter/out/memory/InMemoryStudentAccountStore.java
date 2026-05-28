package com.example.librarydashboard.adapter.out.memory;

import com.example.librarydashboard.port.out.StudentAccountStore;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class InMemoryStudentAccountStore implements StudentAccountStore {

    private final Map<String, Map<String, Object>> studentsByEmail = new LinkedHashMap<>();
    private final Map<String, String> userIdByToken = new LinkedHashMap<>();

    public InMemoryStudentAccountStore() {
        seedStudent("student-001", "김도서", "20240001", "student1@library.com", "password123");
        seedStudent("student-002", "이열람", "20240002", "student2@library.com", "password123");
        seedStudent("student-003", "박자료", "20240003", "student3@library.com", "password123");
        seedStudent("student-004", "최좌석", "20240004", "student4@library.com", "password123");
    }

    @Override
    public Optional<Map<String, Object>> findByEmail(String email) {
        return Optional.ofNullable(studentsByEmail.get(email)).map(LinkedHashMap::new);
    }

    @Override
    public Optional<Map<String, Object>> findById(String userId) {
        return studentsByEmail.values().stream()
                .filter(user -> userId.equals(user.get("id")))
                .findFirst()
                .map(LinkedHashMap::new);
    }

    @Override
    public Map<String, Object> save(Map<String, Object> student) {
        Map<String, Object> copy = new LinkedHashMap<>(student);
        studentsByEmail.put(String.valueOf(copy.get("email")), copy);
        return new LinkedHashMap<>(copy);
    }

    @Override
    public Optional<Map<String, Object>> findByToken(String token) {
        String userId = userIdByToken.get(token);
        if (userId == null) {
            return Optional.empty();
        }
        return findById(userId);
    }

    @Override
    public Optional<Map<String, Object>> findBySelectedSeatId(String seatId) {
        return studentsByEmail.values().stream()
                .filter(user -> seatId.equals(user.get("selectedSeatId")))
                .findFirst()
                .map(LinkedHashMap::new);
    }

    @Override
    public void saveSessionToken(String token, String userId) {
        userIdByToken.put(token, userId);
    }

    @Override
    public void resetForTesting() {
        userIdByToken.clear();
        for (Map<String, Object> student : studentsByEmail.values()) {
            student.put("warningCount", 0);
            student.put("selectedSeatId", null);
        }
    }

    private void seedStudent(String id, String name, String studentId, String email, String password) {
        Map<String, Object> student = new LinkedHashMap<>();
        student.put("id", id);
        student.put("name", name);
        student.put("studentId", studentId);
        student.put("email", email);
        student.put("password", password);
        student.put("warningCount", 0);
        student.put("agreedToPrivacy", true);
        student.put("selectedSeatId", null);
        studentsByEmail.put(email, student);
    }
}
