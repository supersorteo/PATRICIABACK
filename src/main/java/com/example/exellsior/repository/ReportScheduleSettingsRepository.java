package com.example.exellsior.repository;

import com.example.exellsior.entity.ReportScheduleSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportScheduleSettingsRepository extends JpaRepository<ReportScheduleSettings, Long> {
}
