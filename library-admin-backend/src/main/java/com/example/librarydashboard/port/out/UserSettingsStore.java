package com.example.librarydashboard.port.out;

import java.util.Map;

public interface UserSettingsStore {

    Map<String, Object> findByUserIdOrDefault(String userId);

    Map<String, Object> save(String userId, Map<String, Object> settings);
}
