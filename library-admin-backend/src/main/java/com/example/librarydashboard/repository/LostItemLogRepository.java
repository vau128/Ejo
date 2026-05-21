package com.example.librarydashboard.repository;

import com.example.librarydashboard.entity.LostItemLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LostItemLogRepository extends JpaRepository<LostItemLog, Long> {

    List<LostItemLog> findAllByLostItem_Id(Long lostItemId);
}
