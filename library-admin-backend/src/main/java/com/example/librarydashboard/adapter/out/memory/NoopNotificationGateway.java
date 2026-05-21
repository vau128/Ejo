package com.example.librarydashboard.adapter.out.memory;

import com.example.librarydashboard.port.out.NotificationGateway;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NoopNotificationGateway implements NotificationGateway {

    @Override
    public void sendStudentNotification(String userId, String title, String message, Map<String, Object> metadata) {
    }
}
