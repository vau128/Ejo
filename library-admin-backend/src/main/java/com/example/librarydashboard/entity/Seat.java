package com.example.librarydashboard.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "seat")
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seat_id")
    private Long id;

    @Column(name = "seat_num", nullable = false)
    private Integer seatNum;

    @Column(name = "location", nullable = false, length = 100)
    private String location;

    @Column(name = "seat_code", length = 64)
    private String seatCode;

    @Column(name = "pressure")
    private Integer pressure;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "checked_in", nullable = false)
    private boolean checkedIn;

    @Column(name = "occupied", nullable = false)
    private boolean occupied;

    @Column(name = "posture", length = 100)
    private String posture;

    @Column(name = "left_pressure")
    private Integer leftPressure;

    @Column(name = "right_pressure")
    private Integer rightPressure;

    @Column(name = "back_pressure")
    private Integer backPressure;

    @Column(name = "posture_timestamp")
    private LocalDateTime postureTimestamp;

    @Column(name = "vacant_since")
    private LocalDateTime vacantSince;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Seat() {
    }

    public Seat(
            Integer seatNum,
            String location,
            String seatCode,
            Integer pressure,
            String status,
            boolean checkedIn,
            boolean occupied,
            String posture,
            Integer leftPressure,
            Integer rightPressure,
            Integer backPressure,
            LocalDateTime postureTimestamp,
            LocalDateTime vacantSince,
            LocalDateTime updatedAt
    ) {
        this.seatNum = seatNum;
        this.location = location;
        this.seatCode = seatCode;
        this.pressure = pressure;
        this.status = status;
        this.checkedIn = checkedIn;
        this.occupied = occupied;
        this.posture = posture;
        this.leftPressure = leftPressure;
        this.rightPressure = rightPressure;
        this.backPressure = backPressure;
        this.postureTimestamp = postureTimestamp;
        this.vacantSince = vacantSince;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    @PreUpdate
    void ensureUpdatedAt() {
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Integer getSeatNum() {
        return seatNum;
    }

    public void setSeatNum(Integer seatNum) {
        this.seatNum = seatNum;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getSeatCode() {
        return seatCode;
    }

    public void setSeatCode(String seatCode) {
        this.seatCode = seatCode;
    }

    public Integer getPressure() {
        return pressure;
    }

    public void setPressure(Integer pressure) {
        this.pressure = pressure;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isCheckedIn() {
        return checkedIn;
    }

    public void setCheckedIn(boolean checkedIn) {
        this.checkedIn = checkedIn;
    }

    public boolean isOccupied() {
        return occupied;
    }

    public void setOccupied(boolean occupied) {
        this.occupied = occupied;
    }

    public String getPosture() {
        return posture;
    }

    public void setPosture(String posture) {
        this.posture = posture;
    }

    public Integer getLeftPressure() {
        return leftPressure;
    }

    public void setLeftPressure(Integer leftPressure) {
        this.leftPressure = leftPressure;
    }

    public Integer getRightPressure() {
        return rightPressure;
    }

    public void setRightPressure(Integer rightPressure) {
        this.rightPressure = rightPressure;
    }

    public Integer getBackPressure() {
        return backPressure;
    }

    public void setBackPressure(Integer backPressure) {
        this.backPressure = backPressure;
    }

    public LocalDateTime getPostureTimestamp() {
        return postureTimestamp;
    }

    public void setPostureTimestamp(LocalDateTime postureTimestamp) {
        this.postureTimestamp = postureTimestamp;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getVacantSince() {
        return vacantSince;
    }

    public void setVacantSince(LocalDateTime vacantSince) {
        this.vacantSince = vacantSince;
    }
}
