package com.example.librarydashboard.port.out;

public interface ObjectStorageGateway {

    String putObject(String key, byte[] payload, String contentType);
}
