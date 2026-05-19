package com.example.librarydashboard.repository;

import com.example.librarydashboard.entity.SeatUsage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatUsageRepository extends JpaRepository<SeatUsage, Long> {
}
