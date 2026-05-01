package com.example.exellsior.dto;

import com.example.exellsior.entity.ReportScheduleSettings;

import java.time.LocalDate;

public record ReportScheduleConfigDTO(
        boolean enabled,
        String dailySnapshotTime,
        LocalDate lastSnapshotDay
) {
    public static ReportScheduleConfigDTO from(ReportScheduleSettings settings) {
        return new ReportScheduleConfigDTO(
                settings != null && settings.isEnabled(),
                settings != null ? settings.getDailySnapshotTime() : null,
                settings != null ? settings.getLastSnapshotDay() : null
        );
    }
}
