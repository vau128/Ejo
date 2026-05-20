package com.example.librarydashboard.adapter.out.memory;

import com.example.librarydashboard.port.out.LostItemStore;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class InMemoryLostItemStore implements LostItemStore {

    private final List<Map<String, Object>> lostItems = new ArrayList<>();

    public InMemoryLostItemStore() {
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
