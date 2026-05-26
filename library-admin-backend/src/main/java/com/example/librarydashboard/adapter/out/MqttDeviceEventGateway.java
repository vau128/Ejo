package com.example.librarydashboard.adapter.out;

import com.example.librarydashboard.config.IotMqttProperties;
import com.example.librarydashboard.port.out.DeviceEventGateway;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "app.iot.mqtt", name = "enabled", havingValue = "true")
public class MqttDeviceEventGateway implements DeviceEventGateway {

    private final IotMqttProperties properties;
    private final ObjectMapper objectMapper;

    public MqttDeviceEventGateway(IotMqttProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publishSeatStatusChanged(String seatId, String status, Map<String, Object> payload) {
        publish("seat/events/" + seatId, merge(payload, "status", status));
    }

    @Override
    public void publishCommand(String topic, Map<String, Object> payload) {
        publish(topic, payload);
    }

    @Override
    public void recordHeartbeat(String deviceId, Map<String, Object> payload) {
        publish("device/heartbeat/" + deviceId, payload);
    }

    private void publish(String topic, Map<String, Object> payload) {
        try {
            String clientId = properties.clientIdPrefix() + "-" + UUID.randomUUID();
            MqttClient client = new MqttClient(properties.brokerUrl(), clientId);
            try {
                client.connect(connectOptions());
                MqttMessage message = new MqttMessage(serialize(payload));
                message.setQos(properties.qos());
                client.publish(topic, message);
            } finally {
                if (client.isConnected()) {
                    client.disconnect();
                }
                client.close();
            }
        } catch (MqttException e) {
            throw new IllegalStateException(
                    "Failed to publish MQTT message to topic " + topic + " via broker " + properties.brokerUrl(),
                    e
            );
        }
    }

    private MqttConnectOptions connectOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        if (properties.username() != null && !properties.username().isBlank()) {
            options.setUserName(properties.username());
        }
        if (properties.password() != null && !properties.password().isBlank()) {
            options.setPassword(properties.password().toCharArray());
        }
        return options;
    }

    private byte[] serialize(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize MQTT payload", e);
        }
    }

    private Map<String, Object> merge(Map<String, Object> payload, String key, Object value) {
        Map<String, Object> merged = new LinkedHashMap<>(payload);
        merged.putIfAbsent(key, value);
        return merged;
    }
}
