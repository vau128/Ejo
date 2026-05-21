package com.example.librarydashboard.repository;

import com.example.librarydashboard.entity.LostItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LostItemRepository extends JpaRepository<LostItem, Long> {

    List<LostItem> findAllByOrderByDetectedTimeDesc();
}
