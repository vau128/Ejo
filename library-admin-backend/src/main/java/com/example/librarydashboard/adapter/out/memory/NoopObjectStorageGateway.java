package com.example.librarydashboard.adapter.out.memory;

import com.example.librarydashboard.port.out.ObjectStorageGateway;
import org.springframework.stereotype.Component;

@Component
public class NoopObjectStorageGateway implements ObjectStorageGateway {

    @Override
    public String putObject(String key, byte[] payload, String contentType) {
        return "memory://" + key;
    }
}
