package com.example.librarydashboard.adapter.out.memory;

import com.example.librarydashboard.port.out.UserSettingsStore;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class InMemoryUserSettingsStore implements UserSettingsStore {

    private final Map<String, Map<String, Object>> settingsByUserId = new LinkedHashMap<>();

    public InMemoryUserSettingsStore() {
        settingsByUserId.put("student-001", defaultSettings());
    }

    @Override
    public Map<String, Object> findByUserIdOrDefault(String userId) {
        return new LinkedHashMap<>(settingsByUserId.computeIfAbsent(userId, ignored -> defaultSettings()));
    }

    @Override
    public Map<String, Object> save(String userId, Map<String, Object> settings) {
        Map<String, Object> copy = new LinkedHashMap<>(settings);
        settingsByUserId.put(userId, copy);
        return new LinkedHashMap<>(copy);
    }

    private Map<String, Object> defaultSettings() {
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("pushEnabled", true);
        settings.put("seatAlertEnabled", true);
        settings.put("warningAlertEnabled", true);
        return settings;
    }
}
