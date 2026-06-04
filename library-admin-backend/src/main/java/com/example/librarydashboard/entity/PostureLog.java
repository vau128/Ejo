package com.example.librarydashboard.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "posture_log")
public class PostureLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "posture_log_id")
    private Long id;

    @Column(name = "seat_num", nullable = false)
    private Integer seatNum;

    @Column(name = "posture", length = 100)
    private String posture;

    @Column(name = "left_pressure")
    private Integer leftPressure;

    @Column(name = "right_pressure")
    private Integer rightPressure;

    @Column(name = "back_pressure")
    private Integer backPressure;

    @Column(name = "sensor_timestamp")
    private LocalDateTime sensorTimestamp;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected PostureLog() {
    }

    public PostureLog(
            Integer seatNum,
            String posture,
            Integer leftPressure,
            Integer rightPressure,
            Integer backPressure,
            LocalDateTime sensorTimestamp
    ) {
        this.seatNum = seatNum;
        this.posture = posture;
        this.leftPressure = leftPressure;
        this.rightPressure = rightPressure;
        this.backPressure = backPressure;
        this.sensorTimestamp = sensorTimestamp;
    }

    @PrePersist
    void ensureCreatedAt() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Integer getSeatNum() {
        return seatNum;
    }

    public String getPosture() {
        return posture;
    }

    public Integer getLeftPressure() {
        return leftPressure;
    }

    public Integer getRightPressure() {
        return rightPressure;
    }

    public Integer getBackPressure() {
        return backPressure;
    }

    public LocalDateTime getSensorTimestamp() {
        return sensorTimestamp;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
