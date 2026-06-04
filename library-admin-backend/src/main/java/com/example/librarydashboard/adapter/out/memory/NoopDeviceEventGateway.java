package com.example.librarydashboard.adapter.out.memory;

import com.example.librarydashboard.port.out.DeviceEventGateway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "app.iot.mqtt", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoopDeviceEventGateway implements DeviceEventGateway {

    @Override
    public void publishSeatStatusChanged(String seatId, String status, Map<String, Object> payload) {
    }

    @Override
    public void publishCommand(String topic, Map<String, Object> payload) {
    }

    @Override
    public void recordHeartbeat(String deviceId, Map<String, Object> payload) {
    }
}
