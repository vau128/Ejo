package com.example.librarydashboard.service;

import com.example.librarydashboard.dto.IotSeatStatusRequest;
import com.example.librarydashboard.port.out.SeatStore;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class IotService {

    private final SeatStore seatStore;

    public IotService(SeatStore seatStore) {
        this.seatStore = seatStore;
    }

    public Map<String, Object> updateSeatStatus(IotSeatStatusRequest request) {
        Map<String, Object> seat = seatStore.findBySeatId(request.seatId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "좌석을 찾을 수 없습니다."));

        Map<String, Object> updated = new LinkedHashMap<>(seat);
        updated.put("status", normalizeStatus(request.status()));
        if (request.updateTime() != null && !request.updateTime().isBlank()) {
            updated.put("lastUpdated", request.updateTime());
        }
        if (request.imageUrl() != null && !request.imageUrl().isBlank()) {
            updated.put("imageUrl", request.imageUrl());
        }

        seatStore.save(updated);
        return Map.of(
                "message", "IoT 좌석 상태를 반영했습니다.",
                "seatId", request.seatId(),
                "status", updated.get("status")
        );
    }

    private String normalizeStatus(String status) {
        return switch (status) {
            case "정상 사용중" -> "OCCUPIED";
            case "정상 빈좌석" -> "AVAILABLE";
            case "분실물 확인 중", "분실물 확정" -> "ITEM";
            case "사석화 의심 (자리비움)", "사석화 확정", "사석화 (퇴실후 미퇴거)" -> "RESERVED";
            default -> status;
        };
    }
}
