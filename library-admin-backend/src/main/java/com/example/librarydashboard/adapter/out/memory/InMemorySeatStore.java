package com.example.librarydashboard.adapter.out.memory;

import com.example.librarydashboard.port.out.SeatStore;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class InMemorySeatStore implements SeatStore {

    private final List<Map<String, Object>> seats = new ArrayList<>();
    private final Map<String, String> seatIdByUserId = new LinkedHashMap<>();

    public InMemorySeatStore() {
        seats.add(mapOf("seatId", "seat-1", "seatNumber", 1, "status", "AVAILABLE"));
        seats.add(mapOf("seatId", "seat-2", "seatNumber", 2, "status", "AVAILABLE"));
        seats.add(mapOf("seatId", "seat-3", "seatNumber", 3, "status", "AVAILABLE"));
        seats.add(mapOf("seatId", "seat-4", "seatNumber", 4, "status", "AVAILABLE"));
    }

    @Override
    public List<Map<String, Object>> findAll() {
        return seats.stream().map(item -> (Map<String, Object>) new LinkedHashMap<>(item)).toList();
    }

    @Override
    public Optional<Map<String, Object>> findBySeatId(String seatId) {
        return seats.stream()
                .filter(item -> seatId.equals(item.get("seatId")))
                .findFirst()
                .map(LinkedHashMap::new);
    }

    @Override
    public Map<String, Object> save(Map<String, Object> seat) {
        String seatId = String.valueOf(seat.get("seatId"));
        for (int i = 0; i < seats.size(); i++) {
            if (seatId.equals(seats.get(i).get("seatId"))) {
                seats.set(i, new LinkedHashMap<>(seat));
                return new LinkedHashMap<>(seats.get(i));
            }
        }
        seats.add(new LinkedHashMap<>(seat));
        return new LinkedHashMap<>(seat);
    }

    @Override
    public void assignSeatToUser(String seatId, String userId) {
        seatIdByUserId.put(userId, seatId);
    }

    @Override
    public void releaseSeatFromUser(String userId) {
        seatIdByUserId.remove(userId);
    }

    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            map.put(String.valueOf(values[index]), values[index + 1]);
        }
        return map;
    }
}
