package com.example.librarydashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class LibraryAdminBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(LibraryAdminBackendApplication.class, args);
    }
}
