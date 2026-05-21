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
@Table(name = "lost_item")
public class LostItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id")
    private Seat seat;

    @Column(name = "seat_num")
    private Integer seatNum;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "detected_time", nullable = false)
    private LocalDateTime detectedTime;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected LostItem() {
    }

    public LostItem(Seat seat, Integer seatNum, String category, String imageUrl, String status, LocalDateTime detectedTime) {
        this.seat = seat;
        this.seatNum = seatNum;
        this.category = category;
        this.imageUrl = imageUrl;
        this.status = status;
        this.detectedTime = detectedTime;
    }

    @PrePersist
    void ensureCreatedAt() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (detectedTime == null) {
            detectedTime = createdAt;
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

    public String getCategory() {
        return category;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getDetectedTime() {
        return detectedTime;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
