package com.example.librarydashboard.port.out;

import java.util.Map;

public interface NotificationGateway {

    void sendStudentNotification(String userId, String title, String message, Map<String, Object> metadata);
}
