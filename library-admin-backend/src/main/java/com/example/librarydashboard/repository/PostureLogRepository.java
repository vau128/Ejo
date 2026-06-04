package com.example.librarydashboard.repository;

import com.example.librarydashboard.entity.PostureLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostureLogRepository extends JpaRepository<PostureLog, Long> {

    long countByPostureNot(String posture);

    List<PostureLog> findTop40ByOrderByCreatedAtDesc();

    List<PostureLog> findAllBySeatNumAndCreatedAtBetweenOrderByCreatedAtAsc(
            Integer seatNum,
            java.time.LocalDateTime start,
            java.time.LocalDateTime end
    );
}
