package com.example.librarydashboard.port.out;

import java.util.Map;

public interface DeviceEventGateway {

    void publishSeatStatusChanged(String seatId, String status, Map<String, Object> payload);

    void publishCommand(String topic, Map<String, Object> payload);

    void recordHeartbeat(String deviceId, Map<String, Object> payload);
}
