package com.example.librarydashboard.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "warning")
public class Warning {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "warning_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id")
    private Seat seat;

    @Column(name = "seat_num")
    private Integer seatNum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "warning_type", length = 64)
    private String warningType;

    @Column(name = "status", length = 32)
    private String status;

    @Column(name = "message", length = 255)
    private String message;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "warning_time")
    private LocalDateTime warningTime;

    protected Warning() {
    }

    public Warning(Seat seat, Integer seatNum, String warningType, String status, String message, LocalDateTime warningTime) {
        this(seat, null, seatNum, warningType, status, message, warningTime);
    }

    public Warning(Seat seat, User user, Integer seatNum, String warningType, String status, String message, LocalDateTime warningTime) {
        this.seat = seat;
        this.user = user;
        this.seatNum = seatNum;
        this.warningType = warningType;
        this.status = status;
        this.message = message;
        this.warningTime = warningTime;
    }

    @PrePersist
    void ensureCreatedAt() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (warningTime == null) {
            warningTime = createdAt;
        }
    }

    public Long getId() {
        return id;
    }

    public Seat getSeat() {
        return seat;
    }

    public Integer getSeatNum() {
        return seatNum;
    }

    public User getUser() {
        return user;
    }

    public String getWarningType() {
        return warningType;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getWarningTime() {
        return warningTime;
    }
}
