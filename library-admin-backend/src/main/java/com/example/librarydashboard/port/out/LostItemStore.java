package com.example.librarydashboard.port.out;

import java.util.List;
import java.util.Map;

public interface LostItemStore {

    List<Map<String, Object>> findAll();

    Map<String, Object> save(Map<String, Object> lostItem);
}
