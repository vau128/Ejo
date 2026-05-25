package com.example.librarydashboard.port.out;

import java.util.Map;
import java.util.Optional;

public interface StudentAccountStore {

    Optional<Map<String, Object>> findByEmail(String email);

    Optional<Map<String, Object>> findById(String userId);

    Map<String, Object> save(Map<String, Object> student);

    Optional<Map<String, Object>> findByToken(String token);

    Optional<Map<String, Object>> findBySelectedSeatId(String seatId);

    void saveSessionToken(String token, String userId);

    void resetForTesting();
}
