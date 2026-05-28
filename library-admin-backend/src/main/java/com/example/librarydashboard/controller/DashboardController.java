package com.example.librarydashboard.controller;

import com.example.librarydashboard.dto.LostItemStatusRequest;
import com.example.librarydashboard.dto.RuleUpdateRequest;
import com.example.librarydashboard.dto.SeatActionRequest;
import com.example.librarydashboard.dto.SettingsUpdateRequest;
import com.example.librarydashboard.service.DashboardService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/overview")
    public Map<String, Object> overview() {
        return dashboardService.getOverview();
    }

    @GetMapping("/actions")
    public Map<String, Object> actions() {
        return dashboardService.getActions();
    }

    @PostMapping("/actions/warning")
    public Map<String, Object> sendWarning(@Valid @RequestBody SeatActionRequest request) {
        return dashboardService.sendWarning(request.seatId());
    }

    @PostMapping("/actions/release")
    public Map<String, Object> releaseSeat(@Valid @RequestBody SeatActionRequest request) {
        return dashboardService.releaseSeat(request.seatId());
    }

    @PostMapping("/actions/force-checkout")
    public Map<String, Object> forceCheckout(@Valid @RequestBody SeatActionRequest request) {
        return dashboardService.forceCheckout(request.seatId());
    }

    @PostMapping("/actions/resolve")
    public Map<String, Object> resolveSeat(@Valid @RequestBody SeatActionRequest request) {
        return dashboardService.resolveIssue(request.seatId());
    }

    @GetMapping("/alerts/history")
    public Map<String, Object> alertHistory() {
        return dashboardService.getAlertHistory();
    }

    @GetMapping("/alerts/management")
    public Map<String, Object> alertManagement() {
        return dashboardService.getAlertManagement();
    }

    @PatchMapping("/alerts/management/{ruleId}")
    public Map<String, Object> updateAlertRule(@PathVariable String ruleId, @RequestBody RuleUpdateRequest request) {
        return dashboardService.updateAlertRule(ruleId, request.enabled());
    }

    @GetMapping("/stats")
    public Map<String, Object> statistics() {
        return dashboardService.getStatistics();
    }

    @GetMapping("/healthcare/stats")
    public Map<String, Object> healthcareStatistics() {
        return dashboardService.getHealthcareStatistics();
    }

    @GetMapping("/settings")
    public Map<String, Object> settings() {
        return dashboardService.getSettings();
    }

    @PatchMapping("/settings")
    public Map<String, Object> updateSettings(@RequestBody SettingsUpdateRequest request) {
        return dashboardService.updateSettings(request);
    }

    @GetMapping("/seats/zone-3")
    public Map<String, Object> zoneSeats(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search
    ) {
        return dashboardService.getZoneSeats(status, search);
    }

    @GetMapping("/seats/zone-3/{seatId}")
    public Map<String, Object> seatDetail(@PathVariable String seatId) {
        return dashboardService.getSeatDetail(seatId);
    }

    @GetMapping("/seats/abnormal")
    public Map<String, Object> abnormalSeats() {
        return dashboardService.getAbnormalSeats();
    }

    @GetMapping("/lost-items")
    public Map<String, Object> lostItems() {
        return dashboardService.getLostItems();
    }

    @PatchMapping("/lost-items/{itemId}")
    public Map<String, Object> updateLostItemStatus(@PathVariable String itemId, @Valid @RequestBody LostItemStatusRequest request) {
        return dashboardService.updateLostItemStatus(itemId, request.status());
    }

    @GetMapping("/system-status")
    public Map<String, Object> systemStatus() {
        return dashboardService.getSystemStatus();
    }

    @GetMapping("/system-status/sensor-logs")
    public Map<String, Object> sensorLogs() {
        return dashboardService.getSensorLogs();
    }
}
