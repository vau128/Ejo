package com.example.librarydashboard.controller;

import com.example.librarydashboard.dto.seat.AlertResponse;
import com.example.librarydashboard.dto.seat.CheckInStatusResponse;
import com.example.librarydashboard.dto.seat.LostItemResponse;
import com.example.librarydashboard.dto.seat.LostItemSaveRequest;
import com.example.librarydashboard.dto.seat.LostItemScanTriggerRequest;
import com.example.librarydashboard.dto.seat.MessageResponse;
import com.example.librarydashboard.dto.seat.PostureUpdateRequest;
import com.example.librarydashboard.dto.seat.SeatSquattingRequest;
import com.example.librarydashboard.dto.seat.SeatStatusUpdateRequest;
import com.example.librarydashboard.dto.seat.SeatSummaryResponse;
import com.example.librarydashboard.dto.seat.SquattingThresholdRequest;
import com.example.librarydashboard.dto.seat.SquattingThresholdResponse;
import com.example.librarydashboard.dto.seat.SquattingThresholdUpdateResponse;
import com.example.librarydashboard.service.SeatApiService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SeatController {

    private final SeatApiService seatApiService;

    public SeatController(SeatApiService seatApiService) {
        this.seatApiService = seatApiService;
    }

    @PostMapping("/seat/status")
    public MessageResponse updateSeatStatus(@Valid @RequestBody SeatStatusUpdateRequest request) {
        return seatApiService.updateSeatStatus(request);
    }

    @PostMapping("/seat/posture")
    public MessageResponse updatePosture(@Valid @RequestBody PostureUpdateRequest request) {
        return seatApiService.updatePosture(request);
    }

    @PostMapping("/seat/squatting")
    public MessageResponse markSquatting(@Valid @RequestBody SeatSquattingRequest request) {
        return seatApiService.markSquatting(request);
    }

    @PostMapping("/seat/lost-item")
    public MessageResponse saveLostItem(@Valid @RequestBody LostItemSaveRequest request) {
        return seatApiService.saveLostItem(request);
    }

    @GetMapping("/seat/check-in-status/{seatNum}")
    public CheckInStatusResponse getCheckInStatus(@PathVariable int seatNum) {
        return seatApiService.getCheckInStatus(seatNum);
    }

    @GetMapping("/seats")
    public List<SeatSummaryResponse> getSeats() {
        return seatApiService.getSeats();
    }

    @GetMapping("/lost-items")
    public List<LostItemResponse> getLostItems() {
        return seatApiService.getLostItems();
    }

    @GetMapping("/warnings")
    public List<AlertResponse> getWarnings() {
        return seatApiService.getWarnings();
    }

    @GetMapping("/alerts")
    public List<AlertResponse> getAlerts() {
        return seatApiService.getWarnings();
    }

    @GetMapping("/settings/squatting-threshold")
    public SquattingThresholdResponse getSquattingThreshold() {
        return seatApiService.getSquattingThreshold();
    }

    @PostMapping("/admin/lost-item-scan")
    public MessageResponse triggerLostItemScan(@Valid @RequestBody LostItemScanTriggerRequest request) {
        return seatApiService.triggerLostItemScan(request);
    }

    @PutMapping("/settings/squatting-threshold")
    public SquattingThresholdUpdateResponse updateSquattingThreshold(@Valid @RequestBody SquattingThresholdRequest request) {
        return seatApiService.updateSquattingThreshold(request);
    }

    @PostMapping("/testing/reset-demo-state")
    public MessageResponse resetDemoState() {
        return seatApiService.resetDemoState();
    }

    @GetMapping("/db-test")
    public Map<String, Object> dbTest() {
        return seatApiService.getDatabaseTest();
    }
}
