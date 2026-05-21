package com.example.librarydashboard.port.out;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface SeatStore {

    List<Map<String, Object>> findAll();

    Optional<Map<String, Object>> findBySeatId(String seatId);

    Map<String, Object> save(Map<String, Object> seat);

    void assignSeatToUser(String seatId, String userId);

    void releaseSeatFromUser(String userId);
}
