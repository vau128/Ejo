package com.example.librarydashboard.adapter.out.memory;

import com.example.librarydashboard.port.out.DeviceEventGateway;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NoopDeviceEventGateway implements DeviceEventGateway {

    @Override
    public void publishSeatStatusChanged(String seatId, String status, Map<String, Object> payload) {
    }

    @Override
    public void recordHeartbeat(String deviceId, Map<String, Object> payload) {
    }
}
