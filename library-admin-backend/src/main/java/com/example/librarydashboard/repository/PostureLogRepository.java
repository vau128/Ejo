package com.example.librarydashboard.repository;

import com.example.librarydashboard.entity.PostureLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostureLogRepository extends JpaRepository<PostureLog, Long> {

    long countByPostureNot(String posture);
}
