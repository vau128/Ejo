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
@Table(name = "lost_item_log")
public class LostItemLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private LostItem lostItem;

    @Column(name = "detected_object", length = 100)
    private String detectedObject;

    @Column(name = "detected_time")
    private LocalDateTime detectedTime;

    protected LostItemLog() {
    }

    public LostItemLog(LostItem lostItem, String detectedObject) {
        this.lostItem = lostItem;
        this.detectedObject = detectedObject;
    }

    @PrePersist
    void ensureDetectedTime() {
        if (detectedTime == null) {
            detectedTime = LocalDateTime.now();
        }
    }

    public LostItem getLostItem() {
        return lostItem;
    }

    public String getDetectedObject() {
        return detectedObject;
    }
}
