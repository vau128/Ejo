package com.example.librarydashboard.port.out;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface DashboardOperationsStore {

    List<Map<String, Object>> findAllSeats();

    Optional<Map<String, Object>> findSeatById(String seatId);

    Map<String, Object> saveSeat(Map<String, Object> seat);

    List<Map<String, Object>> findAlertHistory();

    void prependAlertHistory(Map<String, Object> history);

    void clearAlertHistory();

    List<Map<String, Object>> findAlertRules();

    Optional<Map<String, Object>> findAlertRuleById(String ruleId);

    Map<String, Object> saveAlertRule(Map<String, Object> rule);

    List<Map<String, Object>> findLostItems();

    Optional<Map<String, Object>> findLostItemById(String itemId);

    Map<String, Object> saveLostItem(Map<String, Object> item);

    void clearLostItems();

    List<Map<String, Object>> findDevices();

    List<Map<String, Object>> findSensorLogs();

    void prependSensorLog(Map<String, Object> log);

    void clearSensorLogs();

    Map<String, Object> getSettings();

    Map<String, Object> saveSettings(Map<String, Object> settings);
}
