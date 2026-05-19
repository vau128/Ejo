package com.example.librarydashboard.repository;

import com.example.librarydashboard.entity.Warning;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WarningRepository extends JpaRepository<Warning, Long> {

    List<Warning> findAllByOrderByCreatedAtDesc();
}
