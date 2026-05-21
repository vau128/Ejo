package com.example.librarydashboard.service;

import com.example.librarydashboard.repository.SeatRepository;
import org.springframework.stereotype.Service;

@Service
public class TemporaryCheckInStatusService implements CheckInStatusService {

    private final SeatRepository seatRepository;

    public TemporaryCheckInStatusService(SeatRepository seatRepository) {
        this.seatRepository = seatRepository;
    }

    @Override
    public boolean isCheckedIn(int seatNum) {
        return seatRepository.findBySeatNum(seatNum)
                .map(seat -> seat.isCheckedIn())
                .orElse(false);
    }
}
