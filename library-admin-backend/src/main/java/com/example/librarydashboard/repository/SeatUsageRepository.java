package com.example.librarydashboard.repository;

import com.example.librarydashboard.entity.SeatUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SeatUsageRepository extends JpaRepository<SeatUsage, Long> {

    Optional<SeatUsage> findFirstByUserIdAndCheckOutTimeIsNullOrderByCheckInTimeDesc(Long userId);

    Optional<SeatUsage> findFirstBySeatIdAndCheckOutTimeIsNullOrderByCheckInTimeDesc(Long seatId);

    List<SeatUsage> findAllByUserIdOrderByCheckInTimeAsc(Long userId);
}
