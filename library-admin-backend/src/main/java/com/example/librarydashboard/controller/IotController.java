package com.example.librarydashboard.controller;

import com.example.librarydashboard.dto.IotSeatStatusRequest;
import com.example.librarydashboard.service.DashboardService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/iot")
public class IotController {

    private final DashboardService dashboardService;

    public IotController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @PostMapping("/seat-status")
    public Map<String, Object> updateSeatStatus(@Valid @RequestBody IotSeatStatusRequest request) {
        return dashboardService.updateSeatStatusFromIot(request);
    }
}
