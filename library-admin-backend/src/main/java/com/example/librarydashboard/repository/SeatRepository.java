package com.example.librarydashboard.repository;

import com.example.librarydashboard.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    Optional<Seat> findBySeatNum(Integer seatNum);
}
