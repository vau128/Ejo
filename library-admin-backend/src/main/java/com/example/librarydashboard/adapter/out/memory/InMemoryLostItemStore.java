package com.example.librarydashboard.adapter.out.memory;

import com.example.librarydashboard.port.out.LostItemStore;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class InMemoryLostItemStore implements LostItemStore {

    private static final DateTimeFormatter LOST_ITEM_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss", Locale.KOREAN);

    private final List<Map<String, Object>> lostItems = new ArrayList<>();

    public InMemoryLostItemStore() {
        lostItems.add(mapOf(
                "reportId", "report-1",
                "seatNumber", 2,
                "detectedAt", LOST_ITEM_FORMATTER.format(LocalDateTime.of(2026, 4, 12, 9, 10)),
                "imageAssetPath", "assets/images/lost_item_1.png",
                "classificationStatus", "보류"
        ));
        lostItems.add(mapOf(
                "reportId", "report-2",
                "seatNumber", 3,
                "detectedAt", LOST_ITEM_FORMATTER.format(LocalDateTime.of(2026, 4, 12, 11, 45)),
                "imageAssetPath", "assets/images/lost_item_2.png",
                "classificationStatus", "보류"
        ));
        lostItems.add(mapOf(
                "reportId", "report-3",
                "seatNumber", 6,
                "detectedAt", LOST_ITEM_FORMATTER.format(LocalDateTime.of(2026, 4, 11, 18, 20)),
                "imageAssetPath", "assets/images/lost_item_3.png",
                "classificationStatus", "보류"
        ));
    }

    @Override
    public List<Map<String, Object>> findAll() {
        return lostItems.stream().map(item -> (Map<String, Object>) new LinkedHashMap<>(item)).toList();
    }

    @Override
    public Map<String, Object> save(Map<String, Object> lostItem) {
        String reportId = String.valueOf(lostItem.get("reportId"));
        for (int i = 0; i < lostItems.size(); i++) {
            if (reportId.equals(lostItems.get(i).get("reportId"))) {
                lostItems.set(i, new LinkedHashMap<>(lostItem));
                return new LinkedHashMap<>(lostItems.get(i));
            }
        }
        lostItems.add(new LinkedHashMap<>(lostItem));
        return new LinkedHashMap<>(lostItem);
    }

    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            map.put(String.valueOf(values[index]), values[index + 1]);
        }
        return map;
    }
}
