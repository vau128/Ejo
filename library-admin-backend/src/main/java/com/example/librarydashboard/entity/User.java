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
@Table(name = "`user`")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "student_id", length = 50, unique = true)
    private String studentId;

    @Column(name = "password", length = 255)
    private String password;

    @Column(name = "role", length = 20)
    private String role;

    @Column(name = "agreed_to_privacy")
    private Boolean agreedToPrivacy;

    @Column(name = "photo", length = 500)
    private String photo;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public User() {
    }

    public User(
            String email,
            String name,
            String studentId,
            String password,
            String role,
            Boolean agreedToPrivacy,
            String photo
    ) {
        this.email = email;
        this.name = name;
        this.studentId = studentId;
        this.password = password;
        this.role = role;
        this.agreedToPrivacy = agreedToPrivacy;
        this.photo = photo;
    }

    @PrePersist
    void ensureCreatedAt() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (role == null || role.isBlank()) {
            role = "USER";
        }
        if (agreedToPrivacy == null) {
            agreedToPrivacy = Boolean.FALSE;
        }
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Boolean getAgreedToPrivacy() {
        return agreedToPrivacy;
    }

    public void setAgreedToPrivacy(Boolean agreedToPrivacy) {
        this.agreedToPrivacy = agreedToPrivacy;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
