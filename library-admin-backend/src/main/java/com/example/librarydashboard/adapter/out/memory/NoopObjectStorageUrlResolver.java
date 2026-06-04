package com.example.librarydashboard.adapter.out.memory;

import com.example.librarydashboard.port.out.ObjectStorageUrlResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.aws", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoopObjectStorageUrlResolver implements ObjectStorageUrlResolver {

    @Override
    public String resolveReadUrl(String storedValue) {
        return storedValue;
    }
}
