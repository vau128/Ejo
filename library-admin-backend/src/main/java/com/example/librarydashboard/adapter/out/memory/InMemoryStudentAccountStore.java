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
        Map<String, Object> student = new LinkedHashMap<>();
        student.put("id", "student-001");
        student.put("name", "김도서");
        student.put("studentId", "20240001");
        student.put("email", "student@library.com");
        student.put("password", "password123");
        student.put("warningCount", 0);
        student.put("agreedToPrivacy", true);
        student.put("selectedSeatId", null);
        studentsByEmail.put("student@library.com", student);
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
}
