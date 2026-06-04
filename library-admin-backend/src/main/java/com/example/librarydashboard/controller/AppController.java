package com.example.librarydashboard.controller;

import com.example.librarydashboard.dto.AppSettingsRequest;
import com.example.librarydashboard.dto.StudentLoginRequest;
import com.example.librarydashboard.dto.StudentSignupRequest;
import com.example.librarydashboard.service.AppService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/app")
public class AppController {

    private final AppService appService;

    public AppController(AppService appService) {
        this.appService = appService;
    }

    @PostMapping("/auth/login")
    public Map<String, Object> login(@Valid @RequestBody StudentLoginRequest request) {
        return appService.login(request);
    }

    @PostMapping("/auth/signup")
    public Map<String, Object> signup(@Valid @RequestBody StudentSignupRequest request) {
        return appService.signup(request);
    }

    @GetMapping("/me")
    public Map<String, Object> me(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Student-Token", required = false) String studentToken
    ) {
        return appService.getCurrentUser(appService.resolveToken(authorization, studentToken));
    }

    @GetMapping("/seats")
    public Map<String, Object> seats(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Student-Token", required = false) String studentToken
    ) {
        return appService.getSeats(appService.resolveToken(authorization, studentToken));
    }

    @PostMapping("/seats/{seatId}/selection")
    public Map<String, Object> toggleSeatSelection(
            @PathVariable String seatId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Student-Token", required = false) String studentToken
    ) {
        return appService.toggleSeatSelection(appService.resolveToken(authorization, studentToken), seatId);
    }

    @GetMapping("/me/seat")
    public Map<String, Object> mySeat(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Student-Token", required = false) String studentToken
    ) {
        return appService.getMySeat(appService.resolveToken(authorization, studentToken));
    }

    @GetMapping("/me/posture-stats")
    public Map<String, Object> postureStats(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Student-Token", required = false) String studentToken
    ) {
        return appService.getMyPostureStats(appService.resolveToken(authorization, studentToken));
    }

    @GetMapping("/me/warnings")
    public Map<String, Object> warnings(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Student-Token", required = false) String studentToken
    ) {
        return appService.getWarnings(appService.resolveToken(authorization, studentToken));
    }

    @GetMapping("/lost-items")
    public Map<String, Object> lostItems(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Student-Token", required = false) String studentToken
    ) {
        return appService.getLostItems(appService.resolveToken(authorization, studentToken));
    }

    @GetMapping("/settings")
    public Map<String, Object> settings(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Student-Token", required = false) String studentToken
    ) {
        return appService.getSettings(appService.resolveToken(authorization, studentToken));
    }

    @PatchMapping("/settings")
    public Map<String, Object> updateSettings(
            @Valid @RequestBody AppSettingsRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Student-Token", required = false) String studentToken
    ) {
        return appService.updateSettings(appService.resolveToken(authorization, studentToken), request);
    }
}
