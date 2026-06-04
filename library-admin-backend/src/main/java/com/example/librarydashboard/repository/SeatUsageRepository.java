package com.example.librarydashboard.repository;

import com.example.librarydashboard.entity.SeatUsage;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SeatUsageRepository extends JpaRepository<SeatUsage, Long> {

    @EntityGraph(attributePaths = {"seat", "user"})
    Optional<SeatUsage> findFirstByUserIdAndCheckOutTimeIsNullOrderByCheckInTimeDesc(Long userId);

    @EntityGraph(attributePaths = {"seat", "user"})
    Optional<SeatUsage> findFirstBySeatIdAndCheckOutTimeIsNullOrderByCheckInTimeDesc(Long seatId);

    @EntityGraph(attributePaths = {"seat", "user"})
    List<SeatUsage> findAllByUserIdOrderByCheckInTimeAsc(Long userId);
}
